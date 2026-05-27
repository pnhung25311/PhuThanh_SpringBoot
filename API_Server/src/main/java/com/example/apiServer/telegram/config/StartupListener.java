package com.example.apiServer.telegram.config;

import org.springframework.boot.CommandLineRunner;
// import org.springframework.boot.context.event.ApplicationReadyEvent;
// import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.example.apiServer.telegram.service.TelegramService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class StartupListener implements CommandLineRunner {
    private final TelegramService telegramService;

    // @EventListener(ApplicationReadyEvent.class)
    // public void onApplicationStart() {
    //     telegramService.send("🟢 Server Spring Boot đã khởi động thành công!");
    // }

    @Override
    public void run(String... args) {
        telegramService.send("🟢 Server started successfully!");
    }
}
