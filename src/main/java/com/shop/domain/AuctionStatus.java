package com.shop.domain;

import java.util.Set;

public enum AuctionStatus {
    OPEN,
    RUNNING,
    PAUSED,
    FINISHED,
    PAID,
    CANCELLED;

    public boolean canTransitionTo(AuctionStatus next) {
        return switch (this) {
            case OPEN     -> Set.of(RUNNING, CANCELLED).contains(next);
            case RUNNING  -> Set.of(FINISHED, CANCELLED).contains(next);
            case PAUSED   -> Set.of(RUNNING, CANCELLED).contains(next);
            case FINISHED -> Set.of(PAID).contains(next);
            case PAID, CANCELLED -> false;
        };
    }
}
