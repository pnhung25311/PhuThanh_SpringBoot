package com.example.apiServer.service.notification;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.apiServer.service.webshocket.NotificationWsService;


@Service
public class NotificationService {

    private final NotificationWsService wsService;

    public NotificationService(NotificationWsService wsService) {
        this.wsService = wsService;
    }

    public void notifyWarehouse(String userName, Long orderId) {

        Map<String, Object> message = new HashMap<>();
        message.put("type", "NEW_ORDER");
        message.put("message", "Bạn có 1 đơn hàng từ " + userName);
        message.put("orderId", orderId);
        message.put("fromUser", userName);

        wsService.sendToDepartment("warehouse", message);
    }
}