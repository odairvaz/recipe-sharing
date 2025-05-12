package com.example.recipesharing.config;

import com.example.recipesharing.service.filestorage.StorageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    private static final Logger LOGGER = LoggerFactory.getLogger(MvcConfig.class);
    private final StorageProperties storageProperties;


    public MvcConfig(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String baseLocation = storageProperties.getBaseLocation();
        Path basePath = Paths.get(baseLocation).toAbsolutePath();
        String resourceLocationPrefix = "file:" + basePath + "/";

        LOGGER.info("Configuring resource handlers. Base storage path: {}", basePath);

        for (StorageType type : StorageType.values()) {
            String webPath = type.getWebPathPrefix() + "**";
            String diskPath = resourceLocationPrefix + type.getSubDirectory() + "/";

            LOGGER.info("Mapping web path {} to disk location {}", webPath, diskPath);
            registry.addResourceHandler(webPath)
                    .addResourceLocations(diskPath);
        }
    }

}
