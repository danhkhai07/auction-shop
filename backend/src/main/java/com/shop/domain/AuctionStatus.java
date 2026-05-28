package com.shop.domain;

import java.util.Set;

public enum AuctionStatus {
    OPEN,
    RUNNING,
    PAUSED,
    FINISHED,
    CANCELLED;

    public boolean canTransitionTo(AuctionStatus next) {
        return switch (this) {
            case OPEN     -> Set.of(RUNNING, CANCELLED).contains(next);
            case RUNNING  -> Set.of(PAUSED, FINISHED, CANCELLED).contains(next);
            case PAUSED   -> Set.of(RUNNING, FINISHED, CANCELLED).contains(next);
            case FINISHED, CANCELLED -> false;
        };
    }
}
