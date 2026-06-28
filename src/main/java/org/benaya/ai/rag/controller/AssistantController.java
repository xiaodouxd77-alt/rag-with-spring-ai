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
@RestController
@RequestMapping(path = "/assistant")
@RequiredArgsConstructor
@Slf4j
public class AssistantController {
    private final ChatGeneratorService chatGeneratorService;
    private final RagService ragService;
    private final KnowledgeBaseService knowledgeBaseService;

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
        Long knowledgeBaseId = knowledgeBaseService.getKbId(kbCode);
        Prompt prompt = ragService.generatePromptFromClientPrompt(clientPrompt, knowledgeBaseId);
        return chatGeneratorService.generate(prompt);
    }
    private String extractContentFromChatResponse(ChatResponse chatResponse) {
        return chatResponse.getResult().getOutput().getContent();
    }

}
