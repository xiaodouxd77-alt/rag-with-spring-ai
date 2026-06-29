package org.benaya.ai.rag.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import org.springframework.ai.document.Document;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
@Slf4j
@Component
@RequiredArgsConstructor
public class VectorStoreRepository {

    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;
    public void addDocuments(List<Document> docsToAdd) {
        vectorStore.add(docsToAdd);
    }

    public List<Document> similaritySearchWithTopK(String prompt, int topK) {
        SearchRequest searchRequest = SearchRequest.query(prompt).withTopK(topK);
        return vectorStore.similaritySearch(searchRequest);
    }

    // 新增方法：按知识库ID过滤搜索
    public List<Document> similaritySearchWithTopK(String prompt, int topK, Long knowledgeBaseId) {
        // 构造过滤条件：只检索指定知识库的文档
        Filter.Expression filter = new FilterExpressionBuilder()
                .eq("knowledgeBaseId", knowledgeBaseId)
                .build();

        SearchRequest searchRequest = SearchRequest.query(prompt)
                .withTopK(topK)
                .withFilterExpression(filter);

        return vectorStore.similaritySearch(searchRequest);
    }
    /**
     * 按文档ID删除所有向量
     */
    public int deleteByDocumentId(Long documentId) {
        log.info("删除文档向量: documentId={}", documentId);

        String sql = "DELETE FROM vector_store WHERE metadata->>'documentId' = ?";
        int deletedCount = jdbcTemplate.update(sql, documentId.toString());

        log.info("文档向量删除成功: documentId={}, 删除了 {} 个向量", documentId, deletedCount);
        return deletedCount;
    }

    /**
     * 按知识库ID删除所有向量
     */
    public int deleteByKnowledgeBaseId(Long knowledgeBaseId) {
        log.info("删除知识库所有向量: knowledgeBaseId={}", knowledgeBaseId);

        String sql = "DELETE FROM vector_store WHERE metadata->>'knowledgeBaseId' = ?";
        int deletedCount = jdbcTemplate.update(sql, knowledgeBaseId.toString());

        log.info("知识库向量删除成功: knowledgeBaseId={}, 删除了 {} 个向量", knowledgeBaseId, deletedCount);
        return deletedCount;
    }
}
