package org.benaya.ai.rag.runner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.benaya.ai.rag.repository.DocumentRepository;
import org.benaya.ai.rag.service.CsvParserService;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.support.ResourcePatternResolver;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(value = {"database.init"}, havingValue = "true")
public class DatabaseInitRunner implements ApplicationRunner {
    private final DocumentRepository documentRepository;
    private final CsvParserService csvParserService;
    @Autowired
    private ResourcePatternResolver resourcePatternResolver;
    @Override
    public void run(ApplicationArguments args) throws Exception {
        Resource[] csvResources =
                resourcePatternResolver.getResources("classpath:data/*.csv");

        log.info("📚 Found {} CSV files to load", csvResources.length);
        if (csvResources.length == 0) {
            log.warn("⚠️ No CSV files found in data/ directory!");
            return;
        }
        Long knowledgeBaseId = 1L;
        // ✅ 最小改动：加上 for 循环遍历每个文件
        for (Resource resource : csvResources) {
            log.info("Loading: {}", resource.getFilename());
            List<Document> documents = csvParserService.getContentFromCsv(resource, knowledgeBaseId);
            log.info("Adding {} documents to vector store for KB {}", documents.size(), knowledgeBaseId);
            documentRepository.addDocuments(documents);

        }
        log.info("✅ All done!");
    }
}
