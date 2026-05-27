package com.example.apiServer.telegram.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import lombok.RequiredArgsConstructor;

import com.example.apiServer.telegram.config.TelegramConfig;

@Service
@RequiredArgsConstructor
public class TelegramService {
    private final TelegramConfig config;
    private final RestTemplate restTemplate = new RestTemplate();

    public void send(String message) {
        String url = "https://api.telegram.org/bot"
                + config.getBotToken()
                + "/sendMessage";

        String uri = UriComponentsBuilder.fromUriString(url)
                .queryParam("chat_id", config.getChatId())
                .queryParam("text", message)
                .build()
                .toUriString();

        restTemplate.getForObject(uri, String.class);
    }

    public void sendToChat(String chatId, String message) {

        String url = "https://api.telegram.org/bot"
                + config.getBotToken()
                + "/sendMessage";

        String uri = UriComponentsBuilder.fromUriString(url)
                .queryParam("chat_id", chatId)
                .queryParam("text", message)
                .build()
                .toUriString();

        restTemplate.getForObject(uri, String.class);
    }
}
