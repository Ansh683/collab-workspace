package com.ansh.collab.controller;

import com.ansh.collab.model.Document;
import com.ansh.collab.model.DocumentMessage;
import com.ansh.collab.model.DocumentVersion;
import com.ansh.collab.repository.DocumentRepository;
import com.ansh.collab.repository.DocumentVersionRepository;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class DocumentWebSocketController {

    private static Set<String> activeUsers = ConcurrentHashMap.newKeySet();

    private final DocumentRepository repo;
    private final DocumentVersionRepository versionRepo;

    // ✅ ONLY ONE constructor
    public DocumentWebSocketController(DocumentRepository repo, DocumentVersionRepository versionRepo) {
        this.repo = repo;
        this.versionRepo = versionRepo;
    }

    @MessageMapping("/typing")
    @SendTo("/topic/typing")
    public DocumentMessage typing(DocumentMessage message) {
        return message;
    }

    @MessageMapping("/join")
    @SendTo("/topic/users")
    public Set<String> join(DocumentMessage message) {
        if (message.getUsername() != null && !message.getUsername().isEmpty()) {
            activeUsers.add(message.getUsername());
        }
        return activeUsers;
    }

    @MessageMapping("/leave")
    @SendTo("/topic/users")
    public Set<String> leave(DocumentMessage message) {
        activeUsers.remove(message.getUsername());
        return activeUsers;
    }

    // ✅ CORRECT EDIT METHOD WITH VERSIONING
    @MessageMapping("/edit")
    @SendTo("/topic/documents")
    public DocumentMessage handleEdit(DocumentMessage message) {

        Document doc = repo.findById(message.getDocumentId()).orElse(null);

        if (doc != null) {

            // 🔥 SAVE OLD VERSION FIRST
            DocumentVersion version = new DocumentVersion();
            version.setDocumentId(doc.getId());
            version.setContent(doc.getContent());
            version.setUsername(message.getUsername());
            version.setTimestamp(LocalDateTime.now());

            versionRepo.save(version);

            // 🔥 THEN UPDATE MAIN DOC
            doc.setContent(message.getContent());
            repo.save(doc);
        }

        return message;
    }
}