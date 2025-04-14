package com.example.recipesharing.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path storageLocation;

    @Autowired
    public FileStorageService(@Value("${file.uploadDir}") String uploadDir) {
        this.storageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            if (!Files.exists(this.storageLocation)) {
                Files.createDirectories(this.storageLocation);
            }
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory to store uploaded files.", ex);
        }
    }

    public String storeFile(MultipartFile file) {
        String originalFileName  = StringUtils.cleanPath(file.getOriginalFilename());
        try {
            if (originalFileName .contains("..")) {
                throw new RuntimeException("Invalid file path sequence: " + originalFileName );
            }
            String uniqueFileName = UUID.randomUUID() + "_" + originalFileName ;
            Path targetLocation = this.storageLocation.resolve(uniqueFileName );
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            return uniqueFileName ;
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + originalFileName + ". Please try again!", ex);
        }
    }
}

