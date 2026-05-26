package com.shop.infra;

import com.shop.application.AuctionRepository;
import com.shop.domain.Auction;
import com.shop.domain.AuctionStatus;
import com.shop.domain.BidTransaction;
import com.shop.domain.Item;
import com.shop.domain.User;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class PostgresAuctionRepo implements AuctionRepository {
    // =================================================================================================================
    // CÁC HẰNG SỐ SQL QUERY
    // =================================================================================================================


    // Câu truy vấn gộp lấy thông tin Auction, Item (sản phẩm), Seller (người bán) và Highest Bidder (người trả giá cao nhất).
    // Sử dụng LEFT JOIN cho Highest Bidder vì lúc mới mở đấu giá có thể chưa có ai đặt giá.
    private static final String SELECT_AUCTION =
            "SELECT a.id as a_id, a.starting_price, a.current_highest_price, a.final_price, a.start_time, a.end_time, a.status, " +
            "i.id as i_id, i.name as i_name, i.description as i_description, " +
            "s.id as s_id, s.username as s_username, " +
            "b.id as b_id, b.username as b_username " +
            "FROM auctions a " +
            "JOIN items i ON a.item_id = i.id " +
            "JOIN users s ON i.seller_id = s.id " +
            "LEFT JOIN users b ON a.current_highest_bidder_id = b.id ";

    private static final String SELECT_AUCTION_BY_ID = SELECT_AUCTION + "WHERE a.id = :id";
    
    private static final String SELECT_ALL_AUCTIONS = SELECT_AUCTION + "ORDER BY a.start_time DESC";

    private static final String SELECT_ACTIVES = SELECT_AUCTION + "WHERE a.status = 'RUNNING'";

    private static final String SELECT_BIDS_BY_AUCTION_ID =
            "SELECT b.id, b.bid_amount, b.timestamp, u.id as u_id, u.username as u_username " +
            "FROM bids b " +
            "JOIN users u ON b.bidder_id = u.id " +
            "WHERE b.auction_id = :auctionId " +
            "ORDER BY b.timestamp ASC";

    private static final String UPDATE_AUCTION_SQL =
            "UPDATE auctions " +
            "SET current_highest_price = :currentHighestPrice, " +
            "    current_highest_bidder_id = :currentHighestBidderId, " +
            "    final_price = :finalPrice, " +
            "    status = :status, " +
            "    updated_at = CURRENT_TIMESTAMP " +
            "WHERE id = :id";

    private static final String INSERT_AUCTION_SQL =
            "INSERT INTO auctions (id, item_id, starting_price, current_highest_price, start_time, end_time, status) " +
            "VALUES (:id, :itemId, :startingPrice, :currentHighestPrice, :startTime, :endTime, :status)";

    private static final String DELETE_AUCTION_SQL = "DELETE FROM auctions WHERE id = :id";

    private static final String INSERT_BID_SQL =
            "INSERT INTO bids (id, auction_id, bidder_id, bid_amount, timestamp) " +
            "VALUES (:id, :auctionId, :bidderId, :bidAmount, :timestamp) " +
            "ON CONFLICT (id) DO NOTHING";

    private final DatabaseClient databaseClient;

    public PostgresAuctionRepo(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    // =================================================================================================================
    // IMPLEMENTATION CỦA AUCTION REPOSITORY
    // =================================================================================================================

    @Override
    public Flux<Auction> getAll() {
        return databaseClient.sql(SELECT_ALL_AUCTIONS)
                .map(this::mapRowToAuction)
                .all()
                .flatMap(this::loadBidsForAuction);
    }

    @Override
    public Mono<Auction> getByID(String id) {
        if (!StringUtils.hasText(id)) return Mono.empty();
        
        return databaseClient.sql(SELECT_AUCTION_BY_ID)
                .bind("id", id)
                .map(this::mapRowToAuction) // Chuyển Row thành Auction object
                .one()
                .flatMap(this::loadBidsForAuction); // Truy vấn tiếp danh sách Lịch sử đấu giá (bids) và nạp vào Auction
    }

    @Override
    public Flux<Auction> getActives() {
        return databaseClient.sql(SELECT_ACTIVES)
                .map(this::mapRowToAuction)
                .all()
                .flatMap(this::loadBidsForAuction); // Xử lý bất đồng bộ để load bids cho từng Auction đang RUNNING
    }

    @Override
    @Transactional
    public Mono<Void> saveAuction(Auction auction) {
        if (auction == null || !StringUtils.hasText(auction.getId())) {
            return Mono.error(new IllegalArgumentException("auction is invalid"));
        }

        // 1. Lấy thông tin người trả giá cao nhất hiện tại (có thể null)
        String bidderId = auction.getCurrentHighestBidder() != null ? auction.getCurrentHighestBidder().getId() : null;

        // 2. Chuẩn bị câu lệnh cập nhật thông tin Auction
        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(UPDATE_AUCTION_SQL)
                .bind("id", auction.getId())
                .bind("currentHighestPrice", auction.getCurrentHighestPrice() != null ? auction.getCurrentHighestPrice() : auction.getStartingPrice())
                .bind("status", auction.getStatus().name());

        if (bidderId != null) {
            spec = spec.bind("currentHighestBidderId", bidderId);
        } else {
            spec = spec.bindNull("currentHighestBidderId", String.class);
        }

        if (auction.getFinalPrice() != null) {
            spec = spec.bind("finalPrice", auction.getFinalPrice());
        } else {
            spec = spec.bindNull("finalPrice", BigDecimal.class);
        }

        Mono<Void> updateAuction = spec.fetch().rowsUpdated().then();

        if (auction.getBidHistory().isEmpty()) {
            return updateAuction; // Nếu chưa có ai đặt giá thì chỉ việc update bảng auction là xong
        }

        // Nếu đã có bids, ta lưu thêm danh sách bids vào bảng trung gian (hoặc bảng bids)
        return updateAuction.then(insertBids(auction.getId(), auction.getBidHistory()));
    }

    @Override
    @Transactional
    public Mono<Void> deleteByID(String id) {
        if (!StringUtils.hasText(id)) return Mono.empty();
        return databaseClient.sql(DELETE_AUCTION_SQL)
                .bind("id", id)
                .fetch()
                .rowsUpdated()
                .then();
    }
    @Override
    public Mono<Boolean> existsByID(String id){
        if (!StringUtils.hasText(id)){
            return Mono.just(false);
        }
        return Mono.just(true);
    }

    @Override
    public Mono<Boolean> existsByItemID(String itemId) {
        if (!StringUtils.hasText(itemId)) {
            return Mono.just(false);
        }

        return databaseClient.sql("SELECT EXISTS (SELECT 1 FROM auctions WHERE item_id = :itemId) AS auction_exists")
                .bind("itemId", itemId)
                .map((row, metadata) -> Boolean.TRUE.equals(row.get("auction_exists", Boolean.class)))
                .one()
                .defaultIfEmpty(false);
    }

    @Override
    @Transactional
    public Mono<Void> newAuction(Auction auction) {
        if (auction == null || !StringUtils.hasText(auction.getId())) {
            return Mono.error(new IllegalArgumentException("auction is invalid"));
        }

        return databaseClient.sql(INSERT_AUCTION_SQL)
                .bind("id", auction.getId())
                .bind("itemId", auction.getItem().getId())
                .bind("startingPrice", auction.getStartingPrice())
                .bind("currentHighestPrice", auction.getCurrentHighestPrice() != null ? auction.getCurrentHighestPrice() : auction.getStartingPrice())
                .bind("startTime", auction.getStartTime())
                .bind("endTime", auction.getEndTime())
                .bind("status", auction.getStatus().name())
                .fetch()
                .rowsUpdated()
                .then();
    }

    // =================================================================================================================
    // CÁC HÀM HELPER VÀ MAPPING
    // =================================================================================================================

    // Hàm lấy danh sách Bids (Lịch sử đặt giá) cho một Auction
    private Mono<Auction> loadBidsForAuction(Auction auction) {
        return databaseClient.sql(SELECT_BIDS_BY_AUCTION_ID)
                .bind("auctionId", auction.getId())
                .map(this::mapRowToBid)
                .all()
                .collectList() // Gom kết quả thành List<BidTransaction>
                .doOnNext(bids -> {
                    if (!bids.isEmpty()) {
                        // Do thuộc tính bidHistory trong Auction là 'private final' và không có hàm setter,
                        // ta bắt buộc phải dùng Reflection để chèn dữ liệu lấy từ DB vào object này.
                        Field field = ReflectionUtils.findField(Auction.class, "bidHistory");
                        if (field != null) {
                            ReflectionUtils.makeAccessible(field);
                            try {
                                @SuppressWarnings("unchecked")
                                List<BidTransaction> history = (List<BidTransaction>) field.get(auction);
                                history.clear(); // Ensure we don't duplicate on re-load
                                history.addAll(bids);
                            } catch (Exception ignored) {
                            }
                        }
                    }
                })
                .thenReturn(auction);
    }

    private Mono<Void> insertBids(String auctionId, List<BidTransaction> bids) {
        return Flux.fromIterable(bids)
                .concatMap(bid -> databaseClient.sql(INSERT_BID_SQL)
                        .bind("id", bid.getTransactionId())
                        .bind("auctionId", auctionId)
                        .bind("bidderId", bid.getBidder().getId())
                        .bind("bidAmount", bid.getBidAmount())
                        .bind("timestamp", bid.getTimestamp())
                        .fetch()
                        .rowsUpdated())
                .then();
    }

    private Auction mapRowToAuction(Row row, RowMetadata metadata) {
        User seller = new User(row.get("s_id", String.class), row.get("s_username", String.class), "");
        Item item = new Item(
                row.get("i_id", String.class),
                row.get("i_name", String.class),
                row.get("i_description", String.class),
                seller
        );

        Auction auction = new Auction(
                row.get("a_id", String.class),
                item,
                row.get("starting_price", BigDecimal.class),
                row.get("start_time", LocalDateTime.class),
                row.get("end_time", LocalDateTime.class)
        );

        String bId = row.get("b_id", String.class);
        if (bId != null) {
            User bidder = new User(bId, row.get("b_username", String.class), "");
            setPrivateField(auction, "currentHighestBidder", bidder);
        }

        BigDecimal currentHighestPrice = row.get("current_highest_price", BigDecimal.class);
        if (currentHighestPrice != null) {
            setPrivateField(auction, "currentHighestPrice", currentHighestPrice);
        }

        BigDecimal finalPrice = row.get("final_price", BigDecimal.class);
        if (finalPrice != null) {
            setPrivateField(auction, "finalPrice", finalPrice);
        }

        String statusStr = row.get("status", String.class);
        if (statusStr != null) {
            setPrivateField(auction, "status", AuctionStatus.valueOf(statusStr));
        }

        return auction;
    }

    private BidTransaction mapRowToBid(Row row, RowMetadata metadata) {
        User bidder = new User(row.get("u_id", String.class), row.get("u_username", String.class), "");
        BidTransaction bid = new BidTransaction(
                row.get("id", String.class),
                bidder,
                row.get("bid_amount", BigDecimal.class)
        );
        
        LocalDateTime timestamp = row.get("timestamp", LocalDateTime.class);
        if (timestamp != null) {
            setPrivateField(bid, "timestamp", timestamp);
        }
        
        return bid;
    }

    // Hàm tiện ích giúp dùng Reflection của Spring để ép gán giá trị cho một biến Private.
    // Việc này giúp ta vượt qua rào cản Encapsulation (đóng gói) của OOP để nạp dữ liệu từ DB vào Domain Model.
    private void setPrivateField(Object target, String fieldName, Object value) {
        Field field = ReflectionUtils.findField(target.getClass(), fieldName);
        if (field != null) {
            ReflectionUtils.makeAccessible(field);
            ReflectionUtils.setField(field, target, value);
        }
    }
}
