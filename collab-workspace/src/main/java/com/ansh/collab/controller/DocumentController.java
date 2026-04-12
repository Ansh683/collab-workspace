package com.ansh.collab.controller;

import com.ansh.collab.model.Document;
import com.ansh.collab.model.DocumentVersion;
import com.ansh.collab.repository.DocumentRepository;
import com.ansh.collab.repository.DocumentVersionRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/documents")
public class DocumentController {

    private final DocumentRepository repo;
    private final DocumentVersionRepository versionRepo;

    // ✅ ONLY ONE CONSTRUCTOR
    public DocumentController(DocumentRepository repo, DocumentVersionRepository versionRepo) {
        this.repo = repo;
        this.versionRepo = versionRepo;
    }
    @PutMapping("/{docId}/restore/{versionId}")
    public Document restoreVersion(@PathVariable Long docId, @PathVariable Long versionId) {

        Document doc = repo.findById(docId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        DocumentVersion version = versionRepo.findById(versionId)
                .orElseThrow(() -> new RuntimeException("Version not found"));

        // restore old content
        doc.setContent(version.getContent());

        return repo.save(doc);
    }
    @GetMapping("/{id}/versions")
    public List<DocumentVersion> getVersions(@PathVariable Long id) {
        return versionRepo.findByDocumentId(id);
    }

    @PostMapping
    public Document create(@RequestBody Document doc) {
        return repo.save(doc);
    }

    @GetMapping("/{id}")
    public Optional<Document> get(@PathVariable Long id) {
        return repo.findById(id);
    }

    @GetMapping
    public List<Document> getAll() {
        return repo.findAll();
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
    public String delete(@PathVariable Long id) {

        if (!repo.existsById(id)) {
            throw new RuntimeException("Document not found with id: " + id);
        }

        repo.deleteById(id);

        return "Document deleted successfully";
    }
}