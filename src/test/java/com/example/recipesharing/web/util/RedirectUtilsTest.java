package com.example.recipesharing.web.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class RedirectUtilsTest {

    @ParameterizedTest
    @CsvSource(nullValues = "NULL", value = {
            "http://localhost:8081/search?keyword=soup, /search?keyword=soup",
            "http://localhost:8081/home,                /home",
            "https://evil.example.com,                  /fallback",
            "//evil.example.com/path,                   /path",
            "http://localhost:8081/login?error,         /fallback",
            "not a uri,                                 /fallback",
            "NULL,                                      /fallback",
    })
    void safeLocalPath(String referer, String expected) {
        assertThat(RedirectUtils.safeLocalPath(referer, "/fallback")).isEqualTo(expected);
    }
}
