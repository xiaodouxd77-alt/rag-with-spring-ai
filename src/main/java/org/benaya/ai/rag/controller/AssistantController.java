package org.benaya.ai.rag.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.benaya.ai.rag.service.ChatGeneratorService;
import org.benaya.ai.rag.service.RagService;
import org.benaya.ai.rag.service.KnowledgeBaseService;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

@RestController
@RequestMapping(path = "/assistant")
@RequiredArgsConstructor
@Slf4j
public class AssistantController {
    private final ChatGeneratorService chatGeneratorService;
    private final RagService ragService;
    private final KnowledgeBaseService knowledgeBaseService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 直接调 Ollama 的测试端点
    @PostMapping(value = "/test/ollama-direct")
    public String testOllamaDirect(@RequestBody String prompt) throws Exception {
        log.info("=== Direct Ollama test, prompt={}", prompt);
        Map<String, Object> request = Map.of(
            "model", "qwen3.5:2b",
            "messages", new Object[]{
                Map.of("role", "user", "content", prompt)
            },
            "stream", false
        );
        String json = restTemplate.postForObject(
            "http://ollama:11434/api/chat",
            request,
            String.class
        );
        log.info("=== Ollama raw response: {}", json);
        JsonNode root = objectMapper.readTree(json);
        String content = root.path("message").path("content").asText();
        log.info("=== Extracted content: {}", content);
        return content;
    }

    @PostMapping(value = "/chat", produces = "text/event-stream")
    public Flux<String> prompt(@RequestBody String clientPrompt,
                               @RequestParam(defaultValue = "1") Long knowledgeBaseId) {
        Prompt prompt = ragService.generatePromptFromClientPrompt(
                clientPrompt,
                knowledgeBaseId
        );
        return chatGeneratorService.generateStream(prompt)
                .map(this::extractContentFromChatResponse)
                .filter(content -> content != null && !content.isEmpty());
    }
    @PostMapping(value = "/chat/by-code", produces = "text/event-stream")
    public Flux<String> promptByCode(
            @RequestBody String clientPrompt,
            @RequestParam String kbCode) {

        // code → id 转换
        Long knowledgeBaseId = knowledgeBaseService.getKbId(kbCode);

        Prompt prompt = ragService.generatePromptFromClientPrompt(
                clientPrompt,
                knowledgeBaseId
        );
        return chatGeneratorService.generateStream(prompt)
                .map(this::extractContentFromChatResponse)
                .filter(content -> content != null && !content.isEmpty());
    }
    @PostMapping(value = "/chat/sync")
    public String promptSync(@RequestBody String clientPrompt,
                             @RequestParam(defaultValue = "1") Long knowledgeBaseId) {
        Prompt prompt = ragService.generatePromptFromClientPrompt(clientPrompt, knowledgeBaseId);
        return chatGeneratorService.generate(prompt);
    }
    @PostMapping(value = "/chat/by-code/sync")
    public String promptByCodeSync(
            @RequestBody String clientPrompt,
            @RequestParam String kbCode) {
        log.info("=== Sync request: kbCode={}, prompt={}", kbCode, clientPrompt);
        Long knowledgeBaseId = knowledgeBaseService.getKbId(kbCode);
        Prompt prompt = ragService.generatePromptFromClientPrompt(clientPrompt, knowledgeBaseId);
        log.info("=== Prompt generated, calling chat model...");
        try {
            String response = chatGeneratorService.generate(prompt);
            log.info("=== Chat response via ChatClient: len={}, content=[{}]",
                    response != null ? response.length() : "null", response);
            return response;
        } catch (Exception e) {
            log.error("=== Chat generation via ChatClient failed", e);
            throw e;
        }
    }

    // 测试端点：直接调 Ollama
    @PostMapping(value = "/test/ollama")
    public String testOllama(@RequestBody String prompt) {
        return chatGeneratorService.generate(prompt);
    }
    private String extractContentFromChatResponse(ChatResponse chatResponse) {
        return chatResponse.getResult().getOutput().getContent();
    }

}
