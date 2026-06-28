package org.benaya.ai.rag.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.benaya.ai.rag.repository.VectorStoreRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagService {
    @Value("classpath:prompts/system-qa.st")
    private Resource systemNdaPrompt;
    @Value("${queries.top-k:2}")
    private int topK;

    private final VectorStoreRepository vectorStoreRepository;
    public Prompt generatePromptFromClientPrompt(String clientPrompt,Long knowledgeBaseId) {
        List<Document> docs = vectorStoreRepository.similaritySearchWithTopK(clientPrompt, topK, knowledgeBaseId);
        log.info("🔍 Retrieved {} documents for query: {}", docs.size(), clientPrompt);
        Message systemMessage = getSystemMessage(docs);
        log.info("System message: {}", systemMessage.getContent());
        UserMessage userMessage = new UserMessage(clientPrompt);
        return new Prompt(List.of(systemMessage, userMessage));
    }
    private Message getSystemMessage(List<Document> similarDocuments) {
        String documents = similarDocuments.stream()
                .map(doc -> {
                    String title = doc.getMetadata().getOrDefault("title", "未知文档").toString();
                    Object pageObj = doc.getMetadata().get("page");
                    String page = pageObj != null ? "第" + pageObj + "页" : "";
                    return "[来源：" + title + page + "]\n" + doc.getContent();
                })
                .collect(Collectors.joining("\n\n"));
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemNdaPrompt);
        return systemPromptTemplate.createMessage(Map.of("documents", documents));
    }
}
