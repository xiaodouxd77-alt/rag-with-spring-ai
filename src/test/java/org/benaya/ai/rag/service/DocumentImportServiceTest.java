// test/java/org/benaya/ai/rag/service/DocumentImportServiceTest.java

package org.benaya.ai.rag.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

@Slf4j
@SpringBootTest
class DocumentImportServiceTest {

    @Autowired
    private DocumentImportService documentImportService;

    @Test
    void importCsv() {
        log.info("========== 开始导入测试 ==========");

        // 1. 准备文件
        Resource resource = new ClassPathResource("data/hr-knowledge.csv");
        Long knowledgeBaseId = 1L;  // HR 知识库的 ID

        log.info("导入文件: {}", resource.getFilename());
        log.info("知识库ID: {}", knowledgeBaseId);

        // 2. 执行导入
        int count = documentImportService.importFromCsv(resource, knowledgeBaseId,null);

        // 3. 验证结果
        log.info("✅ 导入成功！共导入 {} 条记录", count);

        // 简单断言
        assert count > 0 : "应该导入至少一条记录";

        log.info("========== 导入测试完成 ==========");
    }
}