package org.example;

public enum SoftwareVersion {
    V1_01("v1.01", "Версия 1.01"),
    V1_11("v1.11", "Версия 1.11"),
    V2_01("v2.01", "Версия 2.01"),
    V2_11("v2.11", "Версия 2.11");

    private final String folderName;
    private final String displayName;

    SoftwareVersion(String folderName, String displayName) {
        this.folderName = folderName;
        this.displayName = displayName;
    }

    public String getFolderName() {
        return folderName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCode() {
        return name().substring(1).replace('_', '.');
    }

    public boolean isV2() {
        return this == V2_01 || this == V2_11;
    }
}
