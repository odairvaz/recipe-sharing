package com.example.recipesharing.web.util;

import java.net.URI;

public final class RedirectUtils {

    private RedirectUtils() {}

    /**
     * Returns the path (and query) of {@code referer} when it points at this site, otherwise {@code fallback}.
     * Prevents open redirects through a forged Referer header.
     */
    public static String safeLocalPath(String referer, String fallback) {
        if (referer == null) {
            return fallback;
        }
        try {
            URI uri = URI.create(referer);
            String path = uri.getRawPath();
            if (path == null || !path.startsWith("/") || path.startsWith("//") || path.startsWith("/login")) {
                return fallback;
            }
            return uri.getRawQuery() == null ? path : path + "?" + uri.getRawQuery();
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
