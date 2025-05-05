package com.example.recipesharing.config;

import com.example.recipesharing.service.filestorage.StorageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "file.storage")
@Getter
@Setter
@Validated
public class StorageProperties {

    @NotBlank(message = "Base storage location must be set")
    private String baseLocation;

    @NotNull
    private StorageTypeProperties avatar = new StorageTypeProperties();

    @NotNull
    private StorageTypeProperties recipe = new StorageTypeProperties();

    @Getter
    @Setter
    public static class StorageTypeProperties {
        @NotNull(message = "Allowed types must be specified")
        private List<String> allowedTypes;
        private long maxSizeMb;
    }

    public StorageTypeProperties getPropertiesForType(StorageType type) {
        return switch (type) {
            case AVATAR -> avatar;
            case RECIPE_IMAGE -> recipe;
        };
    }
}
