package com.godlights.edplugin.jobs;

public enum JobType {
    MINER("광부"),
    LUMBERJACK("벌목꾼"),
    FARMER("농부"),
    HUNTER("사냥꾼");

    private final String displayName;

    JobType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
