package org.benaya.ai.rag.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import org.springframework.ai.document.Document;
import java.util.List;

import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
@Component
@RequiredArgsConstructor
public class DocumentRepository {

    private final VectorStore vectorStore;

    public void addDocuments(List<Document> docsToAdd) {
        vectorStore.add(docsToAdd);
    }
    public List<Document> similaritySearchWithTopK(String prompt, int topK) {
        SearchRequest searchRequest = SearchRequest.query(prompt).withTopK(topK);
        return vectorStore.similaritySearch(searchRequest);
    }
    // ✅ 新增方法：按知识库ID过滤搜索
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
}
