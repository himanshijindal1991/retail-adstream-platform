
package com.retail.insights_api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CampaignNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(
            CampaignNotFoundException ex) {

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            Map.of(
                "timestamp", Instant.now().toString(),
                "status", 404,
                "error", "Campaign Not Found",
                "message", ex.getMessage()
            )
        );
    }
}
