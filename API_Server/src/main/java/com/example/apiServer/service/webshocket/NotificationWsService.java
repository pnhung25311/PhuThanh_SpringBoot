package com.example.apiServer.service.webshocket;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationWsService {
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationWsService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void sendToDepartment(String department, Object message) {
        messagingTemplate.convertAndSend(
                "/topic/department/" + department,
                message
        );
    }
}
