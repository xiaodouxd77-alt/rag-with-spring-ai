package org.benaya.ai.rag;

import lombok.extern.slf4j.Slf4j;
import org.benaya.ai.rag.repository.DocumentRepository;
import org.benaya.ai.rag.service.RagService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Slf4j
class RagApplicationTests {

	@Autowired
	private DocumentRepository documentRepository;

	@Autowired
	private RagService ragService;

	@Test
	void contextLoads() {
		// 原有的测试
	}

	@Test
	@DisplayName("验证两个知识库的数据隔离效果")
	void testKnowledgeBaseIsolation() {
		log.info("========== 开始测试知识库隔离效果 ==========");

		String testQuestion = "什么是Spring？";

		// 1. 测试知识库 1 (Java)
		log.info("\n📌 测试场景1: 查询知识库 1 (Java)");
		var prompt1 = ragService.generatePromptFromClientPrompt(testQuestion, 1L);
		String systemMsg1 = prompt1.getInstructions().get(0).getContent();
		log.info("返回内容:\n{}", systemMsg1);

		// 2. 测试知识库 2 (Spring)
		log.info("\n📌 测试场景2: 查询知识库 2 (Spring)");
		var prompt2 = ragService.generatePromptFromClientPrompt(testQuestion, 2L);
		String systemMsg2 = prompt2.getInstructions().get(0).getContent();
		log.info("返回内容:\n{}", systemMsg2);

		// 3. 验证隔离效果
		log.info("\n========== 验证隔离效果 ==========");
		List<Document> kb1Docs = documentRepository.similaritySearchWithTopK(testQuestion, 10, 1L);
		List<Document> kb2Docs = documentRepository.similaritySearchWithTopK(testQuestion, 10, 2L);

		log.info("📊 查询结果统计:");
		log.info("  知识库 1: {} 条文档", kb1Docs.size());
		log.info("  知识库 2: {} 条文档", kb2Docs.size());

		// 验证 knowledgeBaseId
		kb1Docs.forEach(doc -> {
			Long kbId = (Long) doc.getMetadata().get("knowledgeBaseId");
			log.info("  ✅ 文档 KB={}: {}", kbId, truncate(doc.getContent(), 50));
			assertThat(kbId).isEqualTo(1L);
		});

		kb2Docs.forEach(doc -> {
			Long kbId = (Long) doc.getMetadata().get("knowledgeBaseId");
			log.info("  ✅ 文档 KB={}: {}", kbId, truncate(doc.getContent(), 50));
			assertThat(kbId).isEqualTo(2L);
		});

		log.info("\n✅ 知识库隔离测试通过！");
		log.info("========== 测试完成 ==========");
	}

	private String truncate(String text, int maxLength) {
		if (text == null) return "";
		return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
	}
}