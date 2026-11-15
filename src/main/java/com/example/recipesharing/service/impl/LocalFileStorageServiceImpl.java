package com.example.recipesharing.service.impl;

import com.example.recipesharing.config.StorageProperties;
import com.example.recipesharing.service.IFileStorageService;
import com.example.recipesharing.service.filestorage.StorageType;
import com.example.recipesharing.web.error.InvalidFileException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@Service
public class LocalFileStorageServiceImpl implements IFileStorageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalFileStorageServiceImpl.class);

    private static final Map<String, String> EXTENSIONS_BY_CONTENT_TYPE = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/gif", ".gif",
            "image/webp", ".webp");

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

        if (allowedTypes != null && !allowedTypes.isEmpty() && !allowedTypes.contains(contentType)) {
            throw new InvalidFileException("Invalid file type: " + contentType + ". Allowed types for " + type + ": " + allowedTypes);
        }

        if (fileSize > maxSizeBytes) {
            throw new InvalidFileException("File size (" + fileSize + " bytes) exceeds the limit of " + props.getMaxSizeMb() + "MB for type " + type);
        }

        String fileExtension = EXTENSIONS_BY_CONTENT_TYPE.get(contentType);
        if (fileExtension == null || !matchesSignature(file, contentType)) {
            throw new InvalidFileException("File content does not match an allowed image type for " + type);
        }

        // The stored name is generated server-side, so the client-supplied filename is never used on disk.
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
                .path(type.getWebPathPrefix())
                .path(storedFilename)
                .toUriString();
    }

    private static boolean matchesSignature(MultipartFile file, String contentType) throws IOException {
        byte[] header = new byte[12];
        int read;
        try (InputStream in = file.getInputStream()) {
            read = in.readNBytes(header, 0, header.length);
        }
        return switch (contentType) {
            case "image/jpeg" -> read >= 3 && (header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF;
            case "image/png" -> read >= 8 && startsWith(header, new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A});
            case "image/gif" -> read >= 6 && startsWith(header, "GIF8".getBytes(StandardCharsets.US_ASCII));
            case "image/webp" -> read >= 12 && startsWith(header, "RIFF".getBytes(StandardCharsets.US_ASCII))
                    && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P';
            default -> false;
        };
    }

    private static boolean startsWith(byte[] data, byte[] prefix) {
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }
}
