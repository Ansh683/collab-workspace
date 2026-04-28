package com.ansh.collab.repository;

import com.ansh.collab.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    // BUG FIX: added this method so getAll() only returns the logged-in user's documents
    List<Document> findByCreatedBy(String createdBy);
}