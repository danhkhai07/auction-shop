package com.shop.infra;

import com.shop.application.UserRepository;
import com.shop.domain.Auction;
import com.shop.domain.AuctionStatus;
import com.shop.domain.BidTransaction;
import com.shop.domain.Item;
import com.shop.domain.Role;
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
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Repository
public class PostgresUserRepo implements UserRepository {
    private static final String SELECT_USER_BY_ID =
            "SELECT u.id, u.username, u.password_hash, u.balance, u.banned, u.banned_reason, u.banned_at, u.banned_by, ur.role_name " + //lấy id,username,pass,role tu bảng user và userrole
                    "FROM users u " +
                    "LEFT JOIN user_roles ur ON ur.user_id = u.id " +
                    "WHERE u.id = :id " +
                    "ORDER BY ur.role_name";

    private static final String SELECT_USER_BY_NAME =
            "SELECT u.id, u.username, u.password_hash, u.balance, u.banned, u.banned_reason, u.banned_at, u.banned_by, ur.role_name " +
                    "FROM users u " +
                    "LEFT JOIN user_roles ur ON ur.user_id = u.id " +
                    "WHERE u.username = :username " +
                    "ORDER BY ur.role_name";

    private static final String SELECT_ALL_USER_ROWS =
            "SELECT u.id, u.username, u.password_hash, u.balance, u.banned, u.banned_reason, u.banned_at, u.banned_by, ur.role_name " +
                    "FROM users u " +
                    "LEFT JOIN user_roles ur ON ur.user_id = u.id " +
                    "ORDER BY u.id, ur.role_name";

    private static final String UPDATE_USER_SQL =
            "UPDATE users " +
                    "SET username = :username, " +
                    "    balance = :balance, " +
                    "    updated_at = CURRENT_TIMESTAMP " +
                    "WHERE id = :id";

    private static final String INSERT_USER_SQL =
            "INSERT INTO users (id, username, password_hash, balance) " +
                    "VALUES (:id, :username, :passwordHash, :balance)";

    private static final String UPDATE_PASSWORD_SQL =
            "UPDATE users " +
                    "SET password_hash = :passwordHash, " +
                    "    updated_at = CURRENT_TIMESTAMP " +
                    "WHERE id = :id";

    private static final String BAN_USER_SQL =
            "UPDATE users " +
                    "SET banned = TRUE, " +
                    "    banned_reason = :reason, " +
                    "    banned_at = CURRENT_TIMESTAMP, " +
                    "    banned_by = :bannedBy, " +
                    "    updated_at = CURRENT_TIMESTAMP " +
                    "WHERE id = :id";

    private static final String UNBAN_USER_SQL =
            "UPDATE users " +
                    "SET banned = FALSE, " +
                    "    banned_reason = NULL, " +
                    "    banned_at = NULL, " +
                    "    banned_by = NULL, " +
                    "    updated_at = CURRENT_TIMESTAMP " +
                    "WHERE id = :id";

    private static final String ADD_BALANCE_SQL =
            "UPDATE users SET balance = balance + :amount, updated_at = CURRENT_TIMESTAMP WHERE id = :id";

    private static final String DEDUCT_BALANCE_SQL =
            "UPDATE users SET balance = balance - :amount, updated_at = CURRENT_TIMESTAMP WHERE id = :id AND balance >= :amount";

    private static final String INSERT_ROLE_SQL =
            "INSERT INTO user_roles (user_id, role_name) " +
                    "VALUES (:userId, :roleName) " +
                    "ON CONFLICT (user_id, role_name) DO NOTHING";

    private static final String SELECT_ITEMS_BY_USER_ID =
            "SELECT i.id AS item_id, i.name, i.description " +
                    "FROM items i " +
                    "WHERE i.seller_id = :userId " +
                    "ORDER BY i.numeric_id";

    private static final String SELECT_AUCTIONS_BY_USER_ID =
            "SELECT a.id as a_id, a.starting_price, a.min_bid_increment, a.current_highest_price, a.final_price, a.start_time, a.end_time, a.status, " +
                    "i.id as i_id, i.name as i_name, i.description as i_description, " +
                    "b.id as b_id, b.username as b_username " +
                    "FROM auctions a " +
                    "JOIN items i ON a.item_id = i.id " +
                    "LEFT JOIN users b ON a.current_highest_bidder_id = b.id " +
                    "WHERE a.seller_id = :userId " +
                    "ORDER BY a.start_time DESC";

    private static final String SELECT_BIDS_BY_AUCTION_ID =
            "SELECT b.id, b.bid_amount, b.timestamp, u.id as u_id, u.username as u_username " +
                    "FROM bids b " +
                    "JOIN users u ON b.bidder_id = u.id " +
                    "WHERE b.auction_id = :auctionId " +
                    "ORDER BY b.timestamp ASC";

    private final DatabaseClient databaseClient;

    public PostgresUserRepo(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @Override
    public Mono<Boolean> existsByID(String id) { // phương thư kiểm tra xem user có tồn tại ko
        if (!StringUtils.hasText(id)) { // ktra xem id có bị rỗng hay không
            return Mono.just(false);
        }

        return databaseClient.sql("SELECT EXISTS (SELECT 1 FROM users WHERE id = :id) AS user_exists")
                .bind("id", id)
                .map((row, metadata) -> Boolean.TRUE.equals(row.get("user_exists", Boolean.class)))
                .one()
                .defaultIfEmpty(false);
    }

    @Override
    public Flux<User> getAll() {
        return databaseClient.sql(SELECT_ALL_USER_ROWS)
                .map((row, metadata) -> new UserRow(
                        row.get("id", String.class),
                        row.get("username", String.class),
                        row.get("password_hash", String.class),
                        row.get("balance", BigDecimal.class),
                        Boolean.TRUE.equals(row.get("banned", Boolean.class)),
                        row.get("banned_reason", String.class),
                        row.get("banned_at", LocalDateTime.class),
                        row.get("banned_by", String.class),
                        row.get("role_name", String.class)
                ))
                .all()
                .bufferUntilChanged(UserRow::id)
                .flatMap(this::mapUser);
    }

    @Override
    public Mono<User> getByID(String id) {
        if (!StringUtils.hasText(id)) {
            return Mono.empty();
        }

        return databaseClient.sql(SELECT_USER_BY_ID)
                .bind("id", id)
                .map((row, metadata) -> new UserRow(
                        row.get("id", String.class),
                        row.get("username", String.class),
                        row.get("password_hash", String.class),
                        row.get("balance", BigDecimal.class),
                        Boolean.TRUE.equals(row.get("banned", Boolean.class)),
                        row.get("banned_reason", String.class),
                        row.get("banned_at", LocalDateTime.class),
                        row.get("banned_by", String.class),
                        row.get("role_name", String.class)
                ))
                .all()
                .collectList()
                .flatMap(this::mapUser);
    }

    @Override
    public Mono<User> getByName(String name) {
        if (!StringUtils.hasText(name)) {
            return Mono.empty();
        }

        return databaseClient.sql(SELECT_USER_BY_NAME)
                .bind("username", name)
                .map((row, metadata) -> new UserRow(
                        row.get("id", String.class),
                        row.get("username", String.class),
                        row.get("password_hash", String.class),
                        row.get("balance", BigDecimal.class),
                        Boolean.TRUE.equals(row.get("banned", Boolean.class)),
                        row.get("banned_reason", String.class),
                        row.get("banned_at", LocalDateTime.class),
                        row.get("banned_by", String.class),
                        row.get("role_name", String.class)
                ))
                .all()
                .collectList()
                .flatMap(this::mapUser);
    }

    @Override
    @Transactional
    public Mono<Void> saveUser(User user) {
        if (user == null || !StringUtils.hasText(user.getId()) || !StringUtils.hasText(user.getUsername())) {
            return Mono.error(new IllegalArgumentException("user is invalid"));
        }

        Mono<Void> updateUser = databaseClient.sql(UPDATE_USER_SQL)
                .bind("id", user.getId())
                .bind("username", user.getUsername())
                .bind("balance", user.getBalance())
                .fetch()
                .rowsUpdated()
                .then();

        if (user.getRoles().isEmpty()) {
            return updateUser;
        }

        return updateUser.then(replaceRoles(user.getId(), user.getRoles()));
    }

    @Override
    @Transactional
    public Mono<Void> deleteByID(String id) {
        if (!StringUtils.hasText(id)) {
            return Mono.empty();
        }

        return databaseClient.sql("DELETE FROM users WHERE id = :id")
                .bind("id", id)
                .fetch()
                .rowsUpdated()
                .then();
    }

    @Override
    @Transactional
    public Mono<Void> newUser(User user) {
        if (user == null || !StringUtils.hasText(user.getId()) || !StringUtils.hasText(user.getUsername())) {
            return Mono.error(new IllegalArgumentException("user is invalid"));
        }
        if (!StringUtils.hasText(user.getPasswordHash())) {
            return Mono.error(new IllegalArgumentException("password hash is invalid"));
        }

        Set<Role> rolesToPersist = user.getRoles().isEmpty() ? Collections.singleton(Role.USER) : user.getRoles();
        if (user.getRoles().isEmpty()) {
            user.addRole(Role.USER);
        }

        return databaseClient.sql(INSERT_USER_SQL)
                .bind("id", user.getId())
                .bind("username", user.getUsername())
                .bind("passwordHash", user.getPasswordHash())
                .bind("balance", user.getBalance())
                .fetch()
                .rowsUpdated()
                .then(insertRoles(user.getId(), rolesToPersist));
    }

    @Override
    @Transactional
    public Mono<Void> changePassword(String id, String password) {
        if (!StringUtils.hasText(id) || !StringUtils.hasText(password)) {
            return Mono.error(new IllegalArgumentException("id or password hash is invalid"));
        }

        return databaseClient.sql(UPDATE_PASSWORD_SQL)
                .bind("id", id)
                .bind("passwordHash", password)
                .fetch()
                .rowsUpdated()
                .then();
    }

    @Override
    @Transactional
    public Mono<Void> banByID(String id, String reason, String bannedBy) {
        if (!StringUtils.hasText(id) || !StringUtils.hasText(bannedBy)) {
            return Mono.error(new IllegalArgumentException("id or bannedBy is invalid"));
        }

        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(BAN_USER_SQL)
                .bind("id", id)
                .bind("bannedBy", bannedBy);

        if (reason != null) {
            spec = spec.bind("reason", reason);
        } else {
            spec = spec.bindNull("reason", String.class);
        }

        return spec.fetch()
                .rowsUpdated()
                .then();
    }

    @Override
    @Transactional
    public Mono<Void> unbanByID(String id) {
        if (!StringUtils.hasText(id)) {
            return Mono.error(new IllegalArgumentException("id is invalid"));
        }

        return databaseClient.sql(UNBAN_USER_SQL)
                .bind("id", id)
                .fetch()
                .rowsUpdated()
                .then();
    }

    @Override
    @Transactional
    public Mono<Void> addBalance(String id, BigDecimal amount) {
        if (!StringUtils.hasText(id) || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Mono.error(new IllegalArgumentException("invalid id or amount"));
        }
        return databaseClient.sql(ADD_BALANCE_SQL)
                .bind("id", id)
                .bind("amount", amount)
                .fetch()
                .rowsUpdated()
                .then();
    }

    @Override
    @Transactional
    public Mono<Void> deductBalance(String id, BigDecimal amount) {
        if (!StringUtils.hasText(id) || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Mono.error(new IllegalArgumentException("invalid id or amount"));
        }
        return databaseClient.sql(DEDUCT_BALANCE_SQL)
                .bind("id", id)
                .bind("amount", amount)
                .fetch()
                .rowsUpdated()
                .flatMap(updatedRows -> {
                    if (updatedRows == 0) {
                        return Mono.error(new IllegalArgumentException("Insufficient balance"));
                    }
                    return Mono.empty();
                });
    }

    private Mono<User> mapUser(List<UserRow> rows) {
        if (rows.isEmpty()) {
            return Mono.empty();
        }

        UserRow firstRow = rows.get(0);
        User user = new User(firstRow.id(), firstRow.username(), "");
        user.setPasswordHash(firstRow.passwordHash());
        user.setBalance(firstRow.balance());
        user.setBanned(firstRow.banned());
        user.setBannedReason(firstRow.bannedReason());
        user.setBannedAt(firstRow.bannedAt());
        user.setBannedBy(firstRow.bannedBy());

        for (UserRow row : rows) {
            if (row.roleName() != null) {
                user.addRole(Role.valueOf(row.roleName()));
            }
        }

        return loadOwnedItems(user)
                .then(loadOwnedAuctions(user))
                .thenReturn(user);
    }

    private Mono<Void> loadOwnedItems(User user) {
        return databaseClient.sql(SELECT_ITEMS_BY_USER_ID)
                .bind("userId", user.getId())
                .map((row, metadata) -> new Item(
                        row.get("item_id", String.class),
                        row.get("name", String.class),
                        row.get("description", String.class),
                        user
                ))
                .all()
                .doOnNext(user::addItem)
                .then();
    }

    private Mono<Void> loadOwnedAuctions(User user) {
        return databaseClient.sql(SELECT_AUCTIONS_BY_USER_ID)
                .bind("userId", user.getId())
                .map((row, metadata) -> mapAuction(row, metadata, user))
                .all()
                .flatMap(this::loadBidsForAuction)
                .doOnNext(user::addAuction)
                .then();
    }

    private Auction mapAuction(Row row, RowMetadata metadata, User seller) {
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
                row.get("min_bid_increment", BigDecimal.class),
                row.get("start_time", LocalDateTime.class),
                row.get("end_time", LocalDateTime.class)
        );

        String bidderId = row.get("b_id", String.class);
        if (bidderId != null) {
            User bidder = new User(bidderId, row.get("b_username", String.class), "");
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

        String status = row.get("status", String.class);
        if (status != null) {
            setPrivateField(auction, "status", AuctionStatus.valueOf(status));
        }

        return auction;
    }

    private Mono<Auction> loadBidsForAuction(Auction auction) {
        return databaseClient.sql(SELECT_BIDS_BY_AUCTION_ID)
                .bind("auctionId", auction.getId())
                .map(this::mapBid)
                .all()
                .collectList()
                .doOnNext(bids -> {
                    Field field = ReflectionUtils.findField(Auction.class, "bidHistory");
                    if (field != null) {
                        ReflectionUtils.makeAccessible(field);
                        @SuppressWarnings("unchecked")
                        List<BidTransaction> history = (List<BidTransaction>) ReflectionUtils.getField(field, auction);
                        if (history != null) {
                            history.clear();
                            history.addAll(bids);
                        }
                    }
                })
                .thenReturn(auction);
    }

    private BidTransaction mapBid(Row row, RowMetadata metadata) {
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

    private void setPrivateField(Object target, String fieldName, Object value) {
        Field field = ReflectionUtils.findField(target.getClass(), fieldName);
        if (field != null) {
            ReflectionUtils.makeAccessible(field);
            ReflectionUtils.setField(field, target, value);
        }
    }

    private Mono<Void> replaceRoles(String userId, Set<Role> roles) {
        return databaseClient.sql("DELETE FROM user_roles WHERE user_id = :userId")
                .bind("userId", userId)
                .fetch()
                .rowsUpdated()
                .then(insertRoles(userId, roles));
    }

    private Mono<Void> insertRoles(String userId, Set<Role> roles) {
        return Flux.fromIterable(roles)
                .concatMap(role -> databaseClient.sql(INSERT_ROLE_SQL)
                        .bind("userId", userId)
                        .bind("roleName", role.name())
                        .fetch()
                        .rowsUpdated())
                .then();
    }

    private static final class UserRow {
        private final String id;
        private final String username;
        private final String passwordHash;
        private final BigDecimal balance;
        private final boolean banned;
        private final String bannedReason;
        private final LocalDateTime bannedAt;
        private final String bannedBy;
        private final String roleName;

        private UserRow(String id, String username, String passwordHash, BigDecimal balance, boolean banned, String bannedReason,
                        LocalDateTime bannedAt, String bannedBy, String roleName) {
            this.id = id;
            this.username = username;
            this.passwordHash = passwordHash;
            this.balance = balance != null ? balance : BigDecimal.ZERO;
            this.banned = banned;
            this.bannedReason = bannedReason;
            this.bannedAt = bannedAt;
            this.bannedBy = bannedBy;
            this.roleName = roleName;
        }

        private String id() {
            return id;
        }

        private String username() {
            return username;
        }

        private String passwordHash() {
            return passwordHash;
        }

        private BigDecimal balance() {
            return balance;
        }

        private boolean banned() {
            return banned;
        }

        private String bannedReason() {
            return bannedReason;
        }

        private LocalDateTime bannedAt() {
            return bannedAt;
        }

        private String bannedBy() {
            return bannedBy;
        }

        private String roleName() {
            return roleName;
        }
    }
}
