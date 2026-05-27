package com.example.apiServer.telegram.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import lombok.RequiredArgsConstructor;
import com.example.apiServer.telegram.service.TelegramService;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/telegram")
public class TelegramController {
    private final TelegramService telegramService;

    @GetMapping("/send")
    public String send() {
        telegramService.send("Hello từ Spring Boot 🚀");
        return "Sent!";
    }

    @GetMapping("/send-user")
    public String sendUser() {
        telegramService.sendToChat("-1003842084129", "Xin chào từ bot 🤖 của hưng đến anh tài");
        return "Sent to other user!";
    }

    @GetMapping("/send-notification")
    public String sendNotification(String message) {
        telegramService.sendToChat("-1003842084129", message);
        return "Sent to other user!";
    }

    @PostMapping("/send-notification-cart")
    public String sendNotificationCart(@RequestBody String message) {
        telegramService.sendToChat("-5195666130", message);
        return "Sent to other user!";
    }
}
