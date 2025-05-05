package com.example.recipesharing.service.filestorage;

public enum StorageType {

    AVATAR("avatars", "/avatars/"),
    RECIPE_IMAGE("recipes", "/recipes/");

    private final String subDirectory;
    private final String webPathPrefix;

    StorageType(String subDirectory, String webPathPrefix) {
        this.subDirectory = subDirectory;
        this.webPathPrefix = webPathPrefix;
    }

    public String getSubDirectory() {
        return subDirectory;
    }

    public String getWebPathPrefix() {
        return webPathPrefix;
    }

}
