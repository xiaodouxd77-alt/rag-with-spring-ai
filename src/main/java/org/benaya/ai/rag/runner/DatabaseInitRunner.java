package org.benaya.ai.rag.runner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.benaya.ai.rag.service.DocumentImportService;
import org.benaya.ai.rag.service.KnowledgeBaseService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(value = {"database.init"}, havingValue = "true")
public class DatabaseInitRunner implements ApplicationRunner {

    private final DocumentImportService documentImportService;
    private final KnowledgeBaseService knowledgeBaseService;
    private final ResourcePatternResolver resourcePatternResolver;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // 1. 确保数据库表已创建（逐条执行 SQL 语句）
        Resource schemaResource = resourcePatternResolver.getResource("classpath:schema.sql");
        String schemaSql = StreamUtils.copyToString(schemaResource.getInputStream(), StandardCharsets.UTF_8);
        for (String statement : schemaSql.split(";")) {
            String trimmed = statement.trim();
            if (!trimmed.isEmpty()) {
                jdbcTemplate.execute(trimmed);
            }
        }
        log.info("✅ Database schema initialized");

        Resource[] csvResources = resourcePatternResolver.getResources("classpath:data/*.csv");

        log.info("📚 Found {} CSV files to load", csvResources.length);
        if (csvResources.length == 0) {
            log.warn("⚠️ No CSV files found in data/ directory!");
            return;
        }

        // ✅ 硬编码映射：文件名 → code
        Map<String, String> fileToCode = Map.of(
                "hr-knowledge.csv", "HR",
                "finance-knowledge.csv", "FINANCE",
                "rd-knowledge.csv", "R&D"
        );

        for (Resource resource : csvResources) {
            String filename = resource.getFilename();
            log.info("📄 Loading: {}", filename);

            // 1. 根据文件名获取 code
            String code = fileToCode.get(filename);
            if (code == null) {
                log.warn("⚠️ No code mapping for file: {}, skipping", filename);
                continue;
            }

            // 2. 通过 KnowledgeBaseService 查询 KB ID
            Long knowledgeBaseId = knowledgeBaseService.getKbId(code);
            log.info("✅ File: {} → Code: {} → KB ID: {}", filename, code, knowledgeBaseId);

            // 3. 通过 DocumentImportService 导入（统一入口）
            // init 阶段没有 Document 记录，documentId 传 null（仅向量化，不追踪状态）
            documentImportService.importFromCsv(resource, knowledgeBaseId, null);
        }
    }
}