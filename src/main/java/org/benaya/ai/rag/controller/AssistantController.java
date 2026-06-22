package org.benaya.ai.rag.controller;

import lombok.RequiredArgsConstructor;
import org.benaya.ai.rag.service.ChatGeneratorService;
import org.benaya.ai.rag.service.RagService;
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
public class AssistantController {
    private final ChatGeneratorService chatGeneratorService;
    private final RagService ragService;

    @PostMapping(value = "/chat", produces = "text/event-stream")
    public Flux<String> prompt(@RequestBody String clientPrompt,
                               @RequestParam(defaultValue = "1") Long knowledgeBaseId) {  // ✅ 只加这个参数
        Prompt prompt = ragService.generatePromptFromClientPrompt(
                clientPrompt,
                knowledgeBaseId  // ✅ 传过去
        );
        return chatGeneratorService.generateStream(prompt)
                .map(this::extractContentFromChatResponse);
    }
    private String extractContentFromChatResponse(ChatResponse chatResponse) {
        return chatResponse.getResult().getOutput().getContent();
    }

}
