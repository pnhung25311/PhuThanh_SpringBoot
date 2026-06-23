package com.example.apiServer.telegram.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.http.HttpStatus;

import lombok.RequiredArgsConstructor;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

// 🎯 THÊM HOẶC SỬA HÀM NÀY ĐỂ CHẶN LỖI FILE TĨNH, KHÔNG CHO BIẾN THÀNH LỖI 500
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Object> handleNoResourceFound(NoResourceFoundException ex) {
        // Trả về 404 mặc định để Spring Boot Admin tự xử lý luồng đi nội bộ của nó
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    // Đây là hàm bắt lỗi chung hiện tại của bạn, giữ nguyên nó
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleAllExceptions(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                             .body("Có lỗi xảy ra: " + ex.getMessage());
    }
}