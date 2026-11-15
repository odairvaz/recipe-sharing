package com.example.recipesharing.web.error;

import com.example.recipesharing.web.util.RedirectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 404/403/500 are rendered by Spring Boot's error controller using the templates in templates/error.
 * This advice only handles errors that deserve a friendlier response than an error page.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxUploadSize(MaxUploadSizeExceededException ex,
                                      @RequestHeader(value = HttpHeaders.REFERER, required = false) String referer,
                                      RedirectAttributes redirectAttributes) {
        LOGGER.debug("Rejected upload: {}", ex.getMessage());
        redirectAttributes.addFlashAttribute("error", "The uploaded file is too large.");
        return "redirect:" + RedirectUtils.safeLocalPath(referer, "/");
    }
}
