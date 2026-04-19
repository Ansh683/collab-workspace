package com.ansh.collab.controller;

import com.ansh.collab.model.Document;
import com.ansh.collab.model.DocumentMessage;
import com.ansh.collab.model.DocumentVersion;
import com.ansh.collab.repository.DocumentRepository;
import com.ansh.collab.repository.DocumentVersionRepository;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class DocumentWebSocketController {

    // docId -> set of usernames currently editing that document
    private static final Map<Long, Set<String>> docUsers = new ConcurrentHashMap<>();

    private final DocumentRepository repo;
    private final DocumentVersionRepository versionRepo;
    private final SimpMessagingTemplate messagingTemplate;

    public DocumentWebSocketController(DocumentRepository repo,
                                       DocumentVersionRepository versionRepo,
                                       SimpMessagingTemplate messagingTemplate) {
        this.repo = repo;
        this.versionRepo = versionRepo;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/typing")
    public void typing(DocumentMessage message, Principal principal) {
        if (message.getDocumentId() == null) return;
        // Use authenticated username, not whatever the client sent
        message.setUsername(principal.getName());
        messagingTemplate.convertAndSend("/topic/typing/" + message.getDocumentId(), message);
    }

    @MessageMapping("/join")
    public void join(DocumentMessage message, Principal principal) {
        Long docId = message.getDocumentId();
        if (docId == null) return;

        String username = principal.getName();  // always trust the server, not the client
        docUsers.computeIfAbsent(docId, k -> ConcurrentHashMap.newKeySet()).add(username);
        messagingTemplate.convertAndSend("/topic/users/" + docId, docUsers.get(docId));
    }

    @MessageMapping("/leave")
    public void leave(DocumentMessage message, Principal principal) {
        Long docId = message.getDocumentId();
        if (docId == null) return;

        String username = principal.getName();
        Set<String> users = docUsers.get(docId);
        if (users != null) {
            users.remove(username);
            messagingTemplate.convertAndSend("/topic/users/" + docId, users);
        }
    }

    @MessageMapping("/edit")
    public void handleEdit(DocumentMessage message, Principal principal) {
        Document doc = repo.findById(message.getDocumentId()).orElse(null);
        String username = principal.getName();

        if (doc != null) {
            // Save old version first
            DocumentVersion version = new DocumentVersion();
            version.setDocumentId(doc.getId());
            version.setContent(doc.getContent());
            version.setUsername(username);
            version.setTimestamp(LocalDateTime.now());
            versionRepo.save(version);

            // Update main doc
            doc.setContent(message.getContent());
            repo.save(doc);
        }

        // Always broadcast with the real server-side username
        message.setUsername(username);
        messagingTemplate.convertAndSend("/topic/documents/" + message.getDocumentId(), message);
    }
}