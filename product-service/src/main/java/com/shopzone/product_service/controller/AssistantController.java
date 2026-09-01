package com.shopzone.product_service.controller;

import com.shopzone.product_service.dto.AssistantRequest;
import com.shopzone.product_service.dto.AssistantResponse;
import com.shopzone.product_service.service.AssistantService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/assistant")
public class AssistantController {

    private final AssistantService assistantService;

    public AssistantController(AssistantService assistantService) {
        this.assistantService = assistantService;
    }

    @PostMapping("/ask")
    public AssistantResponse ask(@RequestBody AssistantRequest request) {
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            return new AssistantResponse("Please type a question first.");
        }
        String answer = assistantService.getAnswer(request);
        return new AssistantResponse(answer);
    }
}