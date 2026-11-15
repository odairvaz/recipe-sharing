package com.example.recipesharing.service.impl;

import com.example.recipesharing.config.StorageProperties;
import com.example.recipesharing.service.filestorage.StorageType;
import com.example.recipesharing.web.error.InvalidFileException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalFileStorageServiceImplTest {

    private static final byte[] PNG_BYTES = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};

    @TempDir
    Path storageRoot;

    private LocalFileStorageServiceImpl storageService;

    @BeforeEach
    void setUp() {
        StorageProperties properties = new StorageProperties();
        properties.setBaseLocation(storageRoot.toString());
        properties.getAvatar().setAllowedTypes(List.of("image/jpeg", "image/png"));
        properties.getAvatar().setMaxSizeMb(1);
        properties.getRecipe().setAllowedTypes(List.of("image/jpeg", "image/png", "image/gif", "image/webp"));
        properties.getRecipe().setMaxSizeMb(1);

        storageService = new LocalFileStorageServiceImpl(properties);
        storageService.init();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void storesValidImageUnderGeneratedNameWithExtensionFromContentType() throws Exception {
        MockMultipartFile file = new MockMultipartFile("f", "../../evil.html", "image/png", PNG_BYTES);

        String url = storageService.storeFile(file, StorageType.AVATAR);

        assertThat(url).startsWith("http://localhost/avatars/").endsWith(".png");
        String storedName = url.substring(url.lastIndexOf('/') + 1);
        assertThat(Files.readAllBytes(storageRoot.resolve("avatars").resolve(storedName))).isEqualTo(PNG_BYTES);
    }

    @Test
    void rejectsContentTypeThatIsNotAllowed() {
        MockMultipartFile gif = new MockMultipartFile("f", "a.gif", "image/gif", "GIF89a".getBytes());

        assertThatThrownBy(() -> storageService.storeFile(gif, StorageType.AVATAR))
                .isInstanceOf(InvalidFileException.class);
    }

    @Test
    void rejectsFileWhoseContentDoesNotMatchDeclaredType() {
        MockMultipartFile fake = new MockMultipartFile("f", "a.png", "image/png", "<script>alert(1)</script>".getBytes());

        assertThatThrownBy(() -> storageService.storeFile(fake, StorageType.RECIPE_IMAGE))
                .isInstanceOf(InvalidFileException.class);
    }

    @Test
    void rejectsFileOverSizeLimit() {
        byte[] big = new byte[1024 * 1024 + 1];
        System.arraycopy(PNG_BYTES, 0, big, 0, PNG_BYTES.length);
        MockMultipartFile file = new MockMultipartFile("f", "big.png", "image/png", big);

        assertThatThrownBy(() -> storageService.storeFile(file, StorageType.RECIPE_IMAGE))
                .isInstanceOf(InvalidFileException.class);
    }

    @Test
    void rejectsEmptyFile() {
        MockMultipartFile empty = new MockMultipartFile("f", "a.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> storageService.storeFile(empty, StorageType.RECIPE_IMAGE))
                .isInstanceOf(InvalidFileException.class);
    }
}
