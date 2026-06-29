package org.benaya.ai.rag.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.benaya.ai.rag.repository.VectorStoreRepository;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentImportService {

    private final CsvParserService csvParserService;
    private final VectorStoreRepository vectorStoreRepository;

    /**
     * 从 CSV 导入文档到向量库
     *
     * @param csvResource    CSV 资源
     * @param knowledgeBaseId 知识库 ID
     * @param documentId      文档 ID（用于向量 metadata，便于按文档删除）
     * @return 导入的 chunks 数量
     */
    public int importFromCsv(Resource csvResource, Long knowledgeBaseId, Long documentId) {
        // 1. 解析 CSV → Spring AI Documents
        List<org.springframework.ai.document.Document> vectorDocs =
                csvParserService.getContentFromCsv(csvResource, knowledgeBaseId, documentId);

        // 2. 确保每条向量记录都携带 documentId（用于按文档删除）
        for (org.springframework.ai.document.Document vd : vectorDocs) {
            vd.getMetadata().put("documentId", documentId);
        }

        // 3. 存入向量库
        vectorStoreRepository.addDocuments(vectorDocs);

        log.info("CSV 导入完成: documentId={}, chunks={}", documentId, vectorDocs.size());
        return vectorDocs.size();
    }
}
