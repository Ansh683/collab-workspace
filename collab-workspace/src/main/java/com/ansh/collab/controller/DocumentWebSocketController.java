package com.ansh.collab.controller;

import com.ansh.collab.model.Document;
import com.ansh.collab.model.DocumentMessage;
import com.ansh.collab.repository.DocumentRepository;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;


@Controller
public class DocumentWebSocketController {


    private final DocumentRepository repo;

    public DocumentWebSocketController(DocumentRepository repo) {
        this.repo = repo;
    }
    @MessageMapping("/typing")
    @SendTo("/topic/typing")
    public DocumentMessage typing(DocumentMessage message) {
        return message;
    }

    @MessageMapping("/edit")
    @SendTo("/topic/documents")
    public DocumentMessage handleEdit(DocumentMessage message) {

        Document doc = repo.findById(message.getDocumentId()).orElse(null);

        if (doc != null) {
            doc.setContent(message.getContent());
            repo.save(doc);
        }

        return message;
    }
}