package com.ansh.collab.model;

public class DocumentMessage {

    private Long documentId;
    private String content;

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    private String username;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
}