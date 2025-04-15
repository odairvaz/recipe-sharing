package com.example.recipesharing.service;

import com.example.recipesharing.web.error.InvalidFileException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface IFileStorageService {

    String storeAvatar(MultipartFile file, String filenamePrefix) throws IOException, InvalidFileException;

}
