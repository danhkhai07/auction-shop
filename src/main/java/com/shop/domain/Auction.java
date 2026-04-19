package com.shop.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Auction {
    private final String id;
    private final Item item;
    private final BigDecimal startingPrice;
    private BigDecimal currentHighestPrice;
    private User currentHighestBidder;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private AuctionStatus status;
    private final List<BidTransaction> bidHistory = new ArrayList<>();

    public Auction(String id, Item item, BigDecimal startingPrice, LocalDateTime startTime, LocalDateTime endTime) {
        if (id == null) throw new IllegalArgumentException("ID phiên đấu giá không hợp lệ.");
        if (item == null) throw new IllegalArgumentException("Sản phẩm đấu giá không được để trống.");
        if (startingPrice == null || startingPrice.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Giá khởi điểm phải lớn hơn hoặc bằng 0.");
        if (startTime == null || endTime == null || !startTime.isBefore(endTime))
            throw new IllegalArgumentException("Thời gian bắt đầu và kết thúc không hợp lệ.");

        this.id = id;
        this.item = item;
        this.startingPrice = startingPrice;
        this.currentHighestPrice = startingPrice;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = AuctionStatus.OPEN; //Trạng thái mặc định
    }

    public void startAuction() {
        if (!this.status.canTransitionTo(AuctionStatus.RUNNING)) {
            throw new IllegalStateException("Không thể bắt đầu phiên đấu giá từ trạng thái hiện tại: " + this.status);
        }
        this.status = AuctionStatus.RUNNING;
    }

    public void placeBid(User bidder, BigDecimal bidAmount) {
        //1. Kiểm tra trạng thái phiên đấu giá
        if (this.status != AuctionStatus.RUNNING) {
            throw new IllegalStateException("Chỉ có thể đặt giá khi phiên đấu giá đang diễn ra.");
        }

        //2. Kiểm tra thời gian
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(startTime) || now.isAfter(endTime)) {
            throw new IllegalStateException("Phiên đấu giá đã hết hạn hoặc chưa bắt đầu.");
        }

        //3. Kiểm tra quyền
        if (!bidder.hasPermission(Permission.PLACE_BID)) {
            throw new SecurityException("Người dùng không có quyền đặt giá.");
        }

        //4. Kiểm tra xem chủ sản phẩm có tự đặt giá không
        if (item.isOwnedBy(bidder)) {
            throw new IllegalArgumentException("Người bán không thể tự đặt giá cho sản phẩm của mình.");
        }

        //5. Kiểm tra giá đặt xem cao hơn giá hiện tại chưa
        if (bidAmount.compareTo(currentHighestPrice) <= 0) {
            throw new IllegalArgumentException("Số tiền đặt giá phải cao hơn mức giá cao nhất hiện tại (" + currentHighestPrice + ").");
        }

        // *Đặt giá*
        String transactionId = UUID.randomUUID().toString();
        BidTransaction newBid = new BidTransaction(transactionId, bidder, bidAmount);

        this.bidHistory.add(newBid);
        this.currentHighestPrice = bidAmount;
        this.currentHighestBidder = bidder;
    }

    public void finishAuction() {
        if (!this.status.canTransitionTo(AuctionStatus.FINISHED)) {
            throw new IllegalStateException("Không thể kết thúc phiên đấu giá từ trạng thái: " + this.status);
        }
        this.status = AuctionStatus.FINISHED;
    }

    public void cancelAuction(User user) {
        if (!user.hasPermission(Permission.CANCEL_AUCTION)) {
            throw new SecurityException("Bạn không có quyền hủy phiên đấu giá này.");
        }

        if (!item.isOwnedBy(user) && !user.hasRole(Role.ADMIN)) {
            throw new SecurityException("Chỉ chủ sản phẩm hoặc Admin mới có thể hủy phiên đấu giá.");
        }

        if (!this.status.canTransitionTo(AuctionStatus.CANCELED)) {
            throw new IllegalStateException("Không thể hủy phiên đấu giá từ trạng thái: " + this.status);
        }
        this.status = AuctionStatus.CANCELED;
    }

    public String getId() { return id; }
    public Item getItem() { return item; }
    public BigDecimal getStartingPrice() { return startingPrice; }
    public BigDecimal getCurrentHighestPrice() { return currentHighestPrice; }
    public User getCurrentHighestBidder() { return currentHighestBidder; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public AuctionStatus getStatus() { return status; }

    public List<BidTransaction> getBidHistory() {
        return Collections.unmodifiableList(bidHistory);
    }
}