package com.example.recipesharing.service;

import com.example.recipesharing.service.filestorage.StorageType;
import com.example.recipesharing.web.error.InvalidFileException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface IFileStorageService {

    String storeFile(MultipartFile file, StorageType type) throws IOException, InvalidFileException;

}
