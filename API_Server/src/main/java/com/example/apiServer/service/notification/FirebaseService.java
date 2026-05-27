package com.example.apiServer.service.notification;

import org.springframework.stereotype.Service;
import com.google.firebase.messaging.*;

@Service
public class FirebaseService {

    public void sendPush(String token,
            String title,
            String body,
            String type,
            String orderId) {

        try {
            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())

                    // dữ liệu để Flutter mở màn hình
                    .putData("type", type)
                    .putData("orderId", orderId)

                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .build())
                    .build();

            FirebaseMessaging.getInstance().send(message);
            System.out.println("PUSH SENT");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}