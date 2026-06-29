package org.benaya.ai.rag.dto;

import org.benaya.ai.rag.model.Document;
import org.benaya.ai.rag.model.DocumentStatus;

import java.time.LocalDateTime;

public record DocumentResponse(
        Long id,
        Long knowledgeBaseId,
        String fileName,
        DocumentStatus status,
        Integer chunkCount,
        Integer tokenCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static DocumentResponse from(Document doc) {
        return new DocumentResponse(
                doc.getId(),
                doc.getKnowledgeBaseId(),
                doc.getFileName(),
                doc.getStatus(),
                doc.getChunkCount(),
                doc.getTokenCount(),
                doc.getCreatedAt(),
                doc.getUpdatedAt()
        );
    }
}
