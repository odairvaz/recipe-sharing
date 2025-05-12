package com.example.recipesharing.service.impl;

import com.example.recipesharing.config.StorageProperties;
import com.example.recipesharing.service.IFileStorageService;
import com.example.recipesharing.service.filestorage.StorageType;
import com.example.recipesharing.web.error.InvalidFileException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;


@Service
public class LocalFileStorageServiceImpl implements IFileStorageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalFileStorageServiceImpl.class);

    private final StorageProperties storageProperties;
    private final Path baseStoragePath;

    public LocalFileStorageServiceImpl(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
        this.baseStoragePath = Paths.get(storageProperties.getBaseLocation()).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(baseStoragePath);
            LOGGER.info("Base storage directory created at: {}", baseStoragePath);

            for (StorageType type : StorageType.values()) {
                Path typePath = baseStoragePath.resolve(type.getSubDirectory()).normalize();
                Files.createDirectories(typePath);
                LOGGER.info("{} storage directory ensured at: {}", type, typePath);
            }
        } catch (IOException ex) {
            LOGGER.error("Could not initialize storage directories under {}", baseStoragePath, ex);
            throw new RuntimeException("Could not initialize storage directories under " + baseStoragePath, ex);
        }
    }

    @Override
    public String storeFile(MultipartFile file, StorageType type) throws IOException, InvalidFileException {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("Upload failed: File is empty or null for type " + type);
        }

        StorageProperties.StorageTypeProperties props = storageProperties.getPropertiesForType(type);
        List<String> allowedTypes = props.getAllowedTypes();
        long maxSizeBytes = props.getMaxSizeMb() * 1024 * 1024;

        String contentType = file.getContentType();
        long fileSize = file.getSize();
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());

        if (allowedTypes != null && !allowedTypes.isEmpty() && !allowedTypes.contains(contentType)) {
            throw new InvalidFileException("Invalid file type: " + contentType + ". Allowed types for " + type + ": " + allowedTypes);
        }

        if (fileSize > maxSizeBytes) {
            throw new InvalidFileException("File size (" + fileSize + " bytes) exceeds the limit of " + props.getMaxSizeMb() + "MB for type " + type);
        }
        if (originalFilename.contains("..")) {
            throw new InvalidFileException("Cannot store file with relative path outside current directory " + originalFilename);
        }


        String fileExtension = "";
        int extensionIndex = originalFilename.lastIndexOf(".");
        if (extensionIndex > 0 && extensionIndex < originalFilename.length() - 1) {
            fileExtension = originalFilename.substring(extensionIndex);
        }
        String storedFilename = UUID.randomUUID() + fileExtension;
        Path targetDirectory = this.baseStoragePath.resolve(type.getSubDirectory()).normalize();
        if (!Files.exists(targetDirectory)) {
            Files.createDirectories(targetDirectory);
        }
        Path targetLocation = targetDirectory.resolve(storedFilename);

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            LOGGER.info("Stored {} file: {} at path: {}", type, storedFilename, targetLocation);
        } catch (IOException ex) {
            LOGGER.error("Could not store {} file {}: ", type, storedFilename, ex);
            throw new IOException("Failed to store " + type + " file " + storedFilename, ex);
        }

        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(type.getWebPathPrefix()) // Use prefix from enum
                .path(storedFilename)
                .toUriString();
    }
}
