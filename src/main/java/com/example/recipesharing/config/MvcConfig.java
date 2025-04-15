package com.example.recipesharing.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    private static final Logger LOGGER = LoggerFactory.getLogger(MvcConfig.class);

    @Value("${file.storage.avatar-location}")
    private String storageLocation;


    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String resolvedPath = Paths.get(storageLocation).toAbsolutePath().normalize().toString();
        if (!resolvedPath.endsWith(System.getProperty("file.separator"))) {
            resolvedPath += System.getProperty("file.separator");
        }
        String resourceLocation = "file:" + resolvedPath;
        String webPath = "/avatars/**";
        registry.addResourceHandler(webPath)
                .addResourceLocations(resourceLocation);

        LOGGER.info("Mapping : {}  to: {} " ,webPath, resourceLocation);
    }
}
