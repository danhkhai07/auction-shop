package com.shop.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
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
    private BigDecimal finalPrice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AuctionStatus status;
    private final List<BidTransaction> bidHistory = new ArrayList<>();

    public Auction(String id, Item item, BigDecimal startingPrice, LocalDateTime startTime, LocalDateTime endTime) {
        if (id == null) throw new IllegalArgumentException("Invalid auction ID.");
        if (item == null) throw new IllegalArgumentException("Auction item cannot be null.");
        if (startingPrice == null || startingPrice.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Starting price must be greater than or equal to 0.");
        if (startTime == null || endTime == null || !startTime.isBefore(endTime))
            throw new IllegalArgumentException("Invalid start and end times.");

        this.id = id;
        this.item = item;
        this.startingPrice = startingPrice;
        this.currentHighestPrice = startingPrice;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = AuctionStatus.OPEN;
    }

    // =================================================================================================================
    //                HÀM CHÍNH
    // =================================================================================================================

    public void startAuction() {
        if (!this.status.canTransitionTo(AuctionStatus.RUNNING)) {
            throw new IllegalStateException("Cannot start the auction from current status: " + this.status);
        }
        this.status = AuctionStatus.RUNNING;
        this.startTime = LocalDateTime.now();
    }

    public void placeBid(User bidder, BigDecimal bidAmount) {
        //1. Kiểm tra trạng thái phiên đấu giá
        if (bidAmount == null || bidAmount.compareTo(currentHighestPrice) <= 0) {
            throw new IllegalArgumentException("Bid amount must be higher than the current price (" + currentHighestPrice + ").");
        }

        //2. Kiểm tra thời gian
        LocalDateTime now = LocalDateTime.now();
        if (!isCurrentlyActive() || isClosed()) {
            throw new IllegalStateException("Auction is not currently active or has already closed.");
        }

        //3. Kiểm tra quyền
        if (bidder == null || !bidder.hasPermission(Permission.PLACE_BID)) {
            throw new SecurityException("Invalid user or insufficient permissions to place a bid.");
        }

        //4. Kiểm tra xem chủ sản phẩm có tự đặt giá không
        if (item.isOwnedBy(bidder)) {
            throw new IllegalArgumentException("Sellers cannot bid on their own items.");
        }

        // Đặt giá
        String transactionId = UUID.randomUUID().toString();
        BidTransaction newBid = new BidTransaction(transactionId, bidder, bidAmount);

        this.bidHistory.add(newBid);
        this.currentHighestPrice = bidAmount;
        this.currentHighestBidder = bidder;
    }

    public void pauseAuction() {
        if (!this.status.canTransitionTo(AuctionStatus.PAUSED)) {
            throw new IllegalStateException("Cannot finish the auction from status: " + this.status);
        }
        this.status = AuctionStatus.PAUSED;
    }

    public void unpauseAuction() {
        if (this.status != AuctionStatus.PAUSED) {
            throw new IllegalStateException("Cannot unpause an auction that is not paused.");
        }
        if (isExpired()) {
            this.status = AuctionStatus.FINISHED;
            return;
        }
        this.status = AuctionStatus.RUNNING;
    }

    public void finishAuction() {
        if (!this.status.canTransitionTo(AuctionStatus.FINISHED)) {
            throw new IllegalStateException("Cannot finish the auction from status: " + this.status);
        }
        this.status = AuctionStatus.FINISHED;
        this.finalPrice = this.currentHighestPrice;
        this.endTime = LocalDateTime.now();
    }

    public void cancelAuction(User user) {
        if (user == null || !user.hasPermission(Permission.CANCEL_AUCTION)) {
            throw new SecurityException("You do not have permission to cancel this auction.");
        }

        if (!item.isOwnedBy(user) && !user.hasRole(Role.ADMIN)) {
            throw new SecurityException("Only the item owner or an admin can cancel this auction.");
        }

        if (!this.status.canTransitionTo(AuctionStatus.CANCELLED)) {
            throw new IllegalStateException("Cannot cancel the auction from status: " + this.status);
        }
        this.status = AuctionStatus.CANCELLED;
        this.finalPrice = BigDecimal.ZERO;
    }

    public void extendEndtime(LocalDateTime newEndtime) {
        if (newEndtime == null || newEndtime.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("New end time must be in the future.");
        }
        this.endTime = newEndtime;
    }

    // =================================================================================================================
    //                HÀM HELPER
    // =================================================================================================================

    public boolean isCurrentlyActive() {
        LocalDateTime now = LocalDateTime.now();
        return this.status == AuctionStatus.RUNNING
                && now.isAfter(startTime)
                && now.isBefore(endTime);
    }

    public boolean isClosed() {
        return this.status == AuctionStatus.FINISHED
                || this.status == AuctionStatus.CANCELLED;
    }

    public boolean isCancelled() {
        return this.status == AuctionStatus.CANCELLED;
    }

    public boolean hasWinner() {
        return this.status == AuctionStatus.FINISHED
                && this.currentHighestBidder != null;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.endTime);
    }


    public List<BidTransaction> getBidsByUser(User user) {
        if (user == null) return List.of();
        return bidHistory.stream()
                .filter(bid -> bid.getBidder().getId().equals(user.getId()))
                .toList();
    }

    //xem chênh giữa giá hiện tại so với ban đầu
    public BigDecimal getPriceIncrease() {
        return currentHighestPrice.subtract(startingPrice);
    }

    //số giây còn lại của phiên đấu giá
    public long getRemainingSeconds() {
        if (isExpired()) return 0;
        return ChronoUnit.SECONDS.between(LocalDateTime.now(), endTime);
    }

    // =================================================================================================================
    //                  GETTERS
    // =================================================================================================================

    public String getId() {
        return id;
    }

    public Item getItem() {
        return item;
    }

    public BigDecimal getStartingPrice() {
        return startingPrice;
    }

    public BigDecimal getCurrentHighestPrice() {
        return currentHighestPrice;
    }

    public User getCurrentHighestBidder() {
        return currentHighestBidder;
    }

    public BigDecimal getFinalPrice() {
        return finalPrice;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public List<BidTransaction> getBidHistory() {
        return Collections.unmodifiableList(bidHistory);
    }
}
