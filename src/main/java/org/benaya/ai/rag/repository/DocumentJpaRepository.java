package org.benaya.ai.rag.repository;

import org.benaya.ai.rag.model.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentJpaRepository extends JpaRepository<Document, Long> {

    Page<Document> findByKnowledgeBaseId(Long knowledgeBaseId, Pageable pageable);

}