package com.example.apiServer.telegram.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.example.apiServer.telegram.service.TelegramService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final TelegramService telegramService;

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleAllException(Exception ex, HttpServletRequest request) {
        // log ra console
        ex.printStackTrace();

        // gửi thông báo Telegram
        telegramService.send("❌ LỖI HỆ THỐNG\n" + ex.getMessage());

        return ResponseEntity.status(500).body("Có lỗi xảy ra: " + ex.getMessage());
    }
}