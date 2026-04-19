package com.ansh.collab.controller;

import com.ansh.collab.model.Document;
import com.ansh.collab.model.DocumentVersion;
import com.ansh.collab.repository.DocumentRepository;
import com.ansh.collab.repository.DocumentVersionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/documents")
public class DocumentController {

    private final DocumentRepository repo;
    private final DocumentVersionRepository versionRepo;

    public DocumentController(DocumentRepository repo, DocumentVersionRepository versionRepo) {
        this.repo = repo;
        this.versionRepo = versionRepo;
    }

    @PostMapping
    public Document create(@RequestBody Document doc, Principal principal) {
        doc.setCreatedBy(principal.getName());  // set owner to logged-in user
        return repo.save(doc);
    }

    @GetMapping
    public List<Document> getAll() {
        return repo.findAll();
    }

    @GetMapping("/{id}")
    public Optional<Document> get(@PathVariable Long id) {
        return repo.findById(id);
    }

    @PutMapping("/{id}")
    public Document update(@PathVariable Long id, @RequestBody Document updatedDoc) {
        Document doc = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found with id: " + id));
        doc.setTitle(updatedDoc.getTitle());
        doc.setContent(updatedDoc.getContent());
        return repo.save(doc);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id, Principal principal) {
        Document doc = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found with id: " + id));

        // Only the owner can delete their document
        if (!doc.getCreatedBy().equals(principal.getName())) {
            return ResponseEntity.status(403).body("Only the document owner can delete it");
        }

        repo.deleteById(id);
        return ResponseEntity.ok("Document deleted successfully");
    }

    @GetMapping("/{id}/versions")
    public List<DocumentVersion> getVersions(@PathVariable Long id) {
        return versionRepo.findByDocumentId(id);
    }

    @PutMapping("/{docId}/restore/{versionId}")
    public Document restoreVersion(@PathVariable Long docId, @PathVariable Long versionId) {
        Document doc = repo.findById(docId)
                .orElseThrow(() -> new RuntimeException("Document not found"));
        DocumentVersion version = versionRepo.findById(versionId)
                .orElseThrow(() -> new RuntimeException("Version not found"));
        doc.setContent(version.getContent());
        return repo.save(doc);
    }
}