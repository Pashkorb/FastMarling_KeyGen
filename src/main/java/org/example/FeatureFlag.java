package org.example;

public enum FeatureFlag {
    FILE_PRINT("FILE_PRINT"),
    SINGLE_PRINT("SINGLE_PRINT"),
    COUNT_PRINT("COUNT_PRINT"),
    ADMIN_PANEL("ADMIN_PANEL"),
    LOGS("LOGS"),
    NESTED_ADMIN("NESTED_ADMIN"),
    REPORTS("REPORTS");

    private final String displayName;

    FeatureFlag(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
