package com.shopzone.product_service.service;

import com.shopzone.product_service.dto.AssistantRequest;
import com.shopzone.product_service.model.Product;
import com.shopzone.product_service.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AssistantService {

    private static final int MAX_PRODUCTS_IN_CONTEXT = 60;
    private static final int MAX_HISTORY_TURNS = 6;
    private static final String FALLBACK_MESSAGE =
            "Sorry, I'm having trouble answering right now. Please try again shortly.";

    private final ProductRepository productRepository;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.model:gemini-3.6-flash}")
    private String geminiModel;

    public AssistantService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public String getAnswer(AssistantRequest request) {
        List<Product> products = productRepository.findAll();
        if (products.size() > MAX_PRODUCTS_IN_CONTEXT) {
            products = products.subList(0, MAX_PRODUCTS_IN_CONTEXT);
        }

        String catalog = products.stream()
                .map(p -> String.format("- %s ($%.2f) | %s | %s",
                        p.getName(), p.getPrice(), p.getCategory(), p.getDescription()))
                .collect(Collectors.joining("\n"));

        String systemInstruction =
                "You are ShopZone's friendly shopping assistant. Only recommend items from the catalog below. " +
                "Be concise (2-4 sentences max unless listing products). Always include exact product name and price " +
                "when recommending. If nothing matches, say so honestly and suggest browsing categories instead.\n\n" +
                "CATALOG:\n" + catalog;

        List<Map<String, Object>> contents = buildConversation(request, systemInstruction);

        String url = String.format(
                "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
                geminiModel, geminiApiKey);

        Map<String, Object> requestBody = Map.of(
                "contents", contents,
                "generationConfig", Map.of(
                        "temperature", 0.4,
                        "maxOutputTokens", 1024
                )
        );

        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            String jsonBody = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(requestBody);

            java.net.http.HttpRequest httpRequest = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(15))
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            java.net.http.HttpResponse<String> httpResponse = client.send(
                    httpRequest, java.net.http.HttpResponse.BodyHandlers.ofString());

                        if (httpResponse.statusCode() != 200) {
                return "DEBUG STATUS " + httpResponse.statusCode() + ": " + httpResponse.body();
            }

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<?, ?> response = mapper.readValue(httpResponse.body(), Map.class);

            List<?> candidates = (List<?>) response.get("candidates");
            if (candidates == null || candidates.isEmpty()) {
                return "I couldn't generate a response to that. Could you rephrase your question?";
            }
            Map<?, ?> firstCandidate = (Map<?, ?>) candidates.get(0);

            Map<?, ?> content = (Map<?, ?>) firstCandidate.get("content");
            if (content == null) {
                return "I couldn't generate a response to that. Could you rephrase your question?";
            }
            List<?> parts = (List<?>) content.get("parts");
            if (parts == null || parts.isEmpty()) {
                return "I couldn't generate a response to that. Could you rephrase your question?";
            }
            Map<?, ?> firstPart = (Map<?, ?>) parts.get(0);
            Object text = firstPart.get("text");
            if (text == null) {
                return "I couldn't generate a response to that. Could you rephrase your question?";
            }
            return text.toString();
                } catch (Exception e) {
            return "DEBUG EXCEPTION: " + e.getClass().getName() + " - " + e.getMessage();
        }
    }

    private List<Map<String, Object>> buildConversation(AssistantRequest request, String systemInstruction) {
        List<Map<String, Object>> contents = new java.util.ArrayList<>();

        // Prime the model with instructions + catalog as the first "user" turn,
        // acknowledged by a placeholder model turn, so subsequent turns feel conversational.
        contents.add(Map.of("role", "user", "parts", List.of(Map.of("text", systemInstruction))));
        contents.add(Map.of("role", "model", "parts", List.of(Map.of("text", "Understood, I'm ready to help customers with the ShopZone catalog."))));

        if (request.getHistory() != null) {
            List<AssistantRequest.ChatTurn> history = request.getHistory();
            int from = Math.max(0, history.size() - MAX_HISTORY_TURNS);
            for (AssistantRequest.ChatTurn turn : history.subList(from, history.size())) {
                String role = "user".equals(turn.getRole()) ? "user" : "model";
                contents.add(Map.of("role", role, "parts", List.of(Map.of("text", turn.getText()))));
            }
        }

        contents.add(Map.of("role", "user", "parts", List.of(Map.of("text", request.getMessage()))));
        return contents;
    }
}