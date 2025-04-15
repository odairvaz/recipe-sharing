package com.example.recipesharing.service.impl;

import com.example.recipesharing.service.IFileStorageService;
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


@Service
public class LocalFileStorageServiceImpl implements IFileStorageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalFileStorageServiceImpl.class);
    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList("image/jpeg", "image/png", "image/gif");
    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024;
    private static final String AVATAR_WEB_PATH_PREFIX = "/avatars/";

    @Value("${file.storage.avatar-location}")
    private String storageLocation;

    private Path fileStoragePath;

    @PostConstruct
    public void init() {
        try {
            fileStoragePath = Paths.get(this.storageLocation).toAbsolutePath().normalize();
            Files.createDirectories(fileStoragePath);
            LOGGER.info("Avatar storage directory created at: {}", fileStoragePath);
        } catch (IOException ex) {
            LOGGER.error("Could not create avatar storage directory: {}", storageLocation, ex);
            throw new RuntimeException("Could not initialize file storage directory: " + storageLocation, ex);
        }
    }

    @Override
    public String storeAvatar(MultipartFile file, String filenamePrefix) throws IOException, InvalidFileException {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("Upload failed: File is empty or null.");
        }

        String contentType = file.getContentType();
        long fileSize = file.getSize();
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());

        if (!ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new InvalidFileException("Invalid file type: " + contentType + ". Allowed types: " + ALLOWED_IMAGE_TYPES);
        }

        if (fileSize > MAX_FILE_SIZE_BYTES) {
            throw new InvalidFileException("File size (" + fileSize + " bytes) exceeds the limit of " + (MAX_FILE_SIZE_BYTES / 1024 / 1024) + "MB.");
        }

        String fileExtension = "";
        int extensionIndex = originalFilename.lastIndexOf(".");
        if (extensionIndex > 0 && extensionIndex < originalFilename.length() - 1) {
            fileExtension = originalFilename.substring(extensionIndex);
        }
        String storedFilename = filenamePrefix + fileExtension;
        Path targetLocation = this.fileStoragePath.resolve(storedFilename);

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            LOGGER.info("Stored avatar file: {} at path: {}", storedFilename, targetLocation);
        } catch (IOException ex) {
            LOGGER.error("Could not store file {}: ", storedFilename, ex);
            throw new IOException("Failed to store file " + storedFilename, ex);
        }

        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(AVATAR_WEB_PATH_PREFIX)
                .path(storedFilename)
                .toUriString();
    }
}
