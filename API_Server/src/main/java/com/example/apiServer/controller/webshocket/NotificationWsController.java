package com.example.apiServer.controller.webshocket;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api/notification")
public class NotificationWsController {

    @MessageMapping("/send")
    public void handleMessage(String message) {
        System.out.println("Client sent: " + message);
    }
}
