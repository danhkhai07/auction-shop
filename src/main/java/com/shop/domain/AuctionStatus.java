package com.shop.domain;

import java.util.Set;

public enum AuctionStatus {
    OPEN,
    RUNNING,
    FINISHED,
    PAID,
    CANCELED;

    public boolean canTransitionTo(AuctionStatus next) {
        return switch (this) {
            case OPEN     -> Set.of(RUNNING, CANCELED).contains(next);
            case RUNNING  -> Set.of(FINISHED, CANCELED).contains(next);
            case FINISHED -> Set.of(PAID).contains(next);
            case PAID, CANCELED -> false;
        };
    }
}
