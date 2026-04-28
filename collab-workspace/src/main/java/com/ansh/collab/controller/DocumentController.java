package com.ansh.collab.controller;

import com.ansh.collab.model.Document;
import com.ansh.collab.model.DocumentVersion;
import com.ansh.collab.repository.DocumentRepository;
import com.ansh.collab.repository.DocumentVersionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/documents")
public class DocumentController {

    private final DocumentRepository repo;
    private final DocumentVersionRepository versionRepo;

    public DocumentController(DocumentRepository repo,
                              DocumentVersionRepository versionRepo) {
        this.repo = repo;
        this.versionRepo = versionRepo;
    }

    // ── CREATE ──
    @PostMapping
    public Document create(@RequestBody Document doc, Principal principal) {
        doc.setCreatedBy(principal.getName());
        return repo.save(doc);
    }

    // ── LIST: only return documents owned by the logged-in user ──
    // BUG FIX: was returning ALL documents to ANY logged-in user
    @GetMapping
    public List<Document> getAll(Principal principal) {
        return repo.findByCreatedBy(principal.getName());
    }

    // ── GET BY ID: enforce ownership ──
    // BUG FIX: was returning any document to any user with the right ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id, Principal principal) {
        return repo.findById(id)
                .map(doc -> {
                    if (!doc.getCreatedBy().equals(principal.getName())) {
                        return ResponseEntity.status(403)
                                .body("You do not have access to this document");
                    }
                    return ResponseEntity.ok(doc);
                })
                .orElse(ResponseEntity.status(404).body("Document not found"));
    }

    // ── UPDATE: enforce ownership ──
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                    @RequestBody Document updated,
                                    Principal principal) {
        return repo.findById(id)
                .map(doc -> {
                    if (!doc.getCreatedBy().equals(principal.getName())) {
                        return ResponseEntity.status(403)
                                .body("Only the document owner can edit it");
                    }
                    doc.setTitle(updated.getTitle());
                    doc.setContent(updated.getContent());
                    return ResponseEntity.ok(repo.save(doc));
                })
                .orElse(ResponseEntity.status(404).body("Document not found"));
    }

    // ── DELETE: enforce ownership ──
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, Principal principal) {
        return repo.findById(id)
                .map(doc -> {
                    if (!doc.getCreatedBy().equals(principal.getName())) {
                        return ResponseEntity.status(403)
                                .body("Only the document owner can delete it");
                    }
                    repo.deleteById(id);
                    return ResponseEntity.ok("Document deleted successfully");
                })
                .orElse(ResponseEntity.status(404).body("Document not found"));
    }

    // ── VERSIONS: enforce ownership ──
    @GetMapping("/{id}/versions")
    public ResponseEntity<?> getVersions(@PathVariable Long id, Principal principal) {
        return repo.findById(id)
                .map(doc -> {
                    if (!doc.getCreatedBy().equals(principal.getName())) {
                        return ResponseEntity.status(403)
                                .body("You do not have access to this document's versions");
                    }
                    return ResponseEntity.ok(versionRepo.findByDocumentId(id));
                })
                .orElse(ResponseEntity.status(404).body("Document not found"));
    }

    // ── RESTORE VERSION: enforce ownership ──
    @PutMapping("/{id}/restore/{versionId}")
    public ResponseEntity<?> restoreVersion(@PathVariable Long id,
                                            @PathVariable Long versionId,
                                            Principal principal) {
        Document doc = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        if (!doc.getCreatedBy().equals(principal.getName())) {
            return ResponseEntity.status(403).body("Only the document owner can restore versions");
        }

        DocumentVersion version = versionRepo.findById(versionId)
                .orElseThrow(() -> new RuntimeException("Version not found"));

        doc.setContent(version.getContent());
        return ResponseEntity.ok(repo.save(doc));
    }
}