package org.benaya.ai.rag.service;

import lombok.RequiredArgsConstructor;
import org.benaya.ai.rag.exception.ResourceNotFoundException;
import org.benaya.ai.rag.repository.KnowledgeBaseRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {
    private final KnowledgeBaseRepository knowledgeBaseRepository;

    public Long getKbId(String code) {
        return knowledgeBaseRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Knowledge base not found: " + code))
                .getId();
    }
}
