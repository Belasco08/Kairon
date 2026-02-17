package com.kairon.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    // Método genérico para avisar que algo mudou
    public void notifyUpdate(String companyId, String type) {
        // Envia para quem estiver inscrito no tópico daquela empresa
        // Ex: /topic/updates/123-456
        String destination = "/topic/updates/" + companyId;

        // Enviamos um JSON simples dizendo O QUE mudou
        UpdateMessage msg = new UpdateMessage(type);

        System.out.println("🔔 Enviando notificação WebSocket para: " + destination);
        messagingTemplate.convertAndSend(destination, msg);
    }

    // Classe interna simples para a mensagem
    public record UpdateMessage(String type) {}
    // Types: "APPOINTMENT_NEW", "FINANCIAL_UPDATE", "CLIENT_UPDATE"
}