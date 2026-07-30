package com.godlights.edplugin.bounty;

import java.util.UUID;

public final class Bounty {

    private final UUID target;
    private double amount;
    private long placedAt;

    public Bounty(UUID target, double amount, long placedAt) {
        this.target = target;
        this.amount = amount;
        this.placedAt = placedAt;
    }

    public UUID target() {
        return target;
    }

    public double amount() {
        return amount;
    }

    public long placedAt() {
        return placedAt;
    }

    public void add(double extra, long now) {
        this.amount += extra;
        this.placedAt = now;
    }

    public boolean isExpired(long now, long expireMillis) {
        return now - placedAt >= expireMillis;
    }
}
