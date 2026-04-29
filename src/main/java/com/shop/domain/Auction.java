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
    private BigDecimal finalPrice;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
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

    public void startAuction() {
        if (!this.status.canTransitionTo(AuctionStatus.RUNNING)) {
            throw new IllegalStateException("Cannot start the auction from current status: " + this.status);
        }
        this.status = AuctionStatus.RUNNING;
    }

    public void placeBid(User bidder, BigDecimal bidAmount) {
        // 1. Check bid amount first (Fast validation)
        if (bidAmount == null || bidAmount.compareTo(currentHighestPrice) <= 0) {
            throw new IllegalArgumentException("Bid amount must be higher than the current price (" + currentHighestPrice + ").");
        }

        // 2. Check status and time simultaneously
        LocalDateTime now = LocalDateTime.now();
        if (this.status != AuctionStatus.RUNNING || now.isBefore(startTime) || now.isAfter(endTime)) {
            throw new IllegalStateException("Auction is not currently active or has already closed.");
        }

        // 3. Check user permissions
        if (bidder == null || !bidder.hasPermission(Permission.PLACE_BID)) {
            throw new SecurityException("Invalid user or insufficient permissions to place a bid.");
        }

        // 4. Check ownership (Seller cannot bid on their own item)
        if (item.isOwnedBy(bidder)) {
            throw new IllegalArgumentException("Sellers cannot bid on their own items.");
        }

        // Execute bid
        String transactionId = UUID.randomUUID().toString();
        BidTransaction newBid = new BidTransaction(transactionId, bidder, bidAmount);

        this.bidHistory.add(newBid);
        this.currentHighestPrice = bidAmount;
        this.currentHighestBidder = bidder;
    }

    public void finishAuction() {
        if (!this.status.canTransitionTo(AuctionStatus.FINISHED)) {
            throw new IllegalStateException("Cannot finish the auction from status: " + this.status);
        }

        this.status = AuctionStatus.FINISHED;
        this.finalPrice = this.currentHighestPrice;
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

    ArrayList<Auction> auctions = new ArrayList<>();

    public String getId() { return id; }
    public Item getItem() { return item; }
    public BigDecimal getStartingPrice() { return startingPrice; }
    public BigDecimal getCurrentHighestPrice() { return currentHighestPrice; }
    public User getCurrentHighestBidder() { return currentHighestBidder; }
    public BigDecimal getFinalPrice() { return finalPrice; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public AuctionStatus getStatus() { return status; }

    public List<BidTransaction> getBidHistory() {
        return Collections.unmodifiableList(bidHistory);
    }
}