package org.benaya.ai.rag.repository;

import org.benaya.ai.rag.model.KnowledgeBase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, Long> {
    Optional<KnowledgeBase> findByCode(String code);
}
