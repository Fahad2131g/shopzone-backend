package com.shopzone.product_service.service;

import com.shopzone.product_service.dto.AssistantRequest;
import com.shopzone.product_service.model.Product;
import com.shopzone.product_service.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AssistantService {

    private static final int MAX_PRODUCTS_IN_CONTEXT = 60;
    private static final int MAX_HISTORY_TURNS = 6;

    private final ProductRepository productRepository;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.model:gemini-3.6-flash}")
    private String geminiModel;

    private final RestClient restClient = RestClient.builder()
            .requestFactory(clientRequestFactory())
            .build();

    public AssistantService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    private static org.springframework.http.client.ClientHttpRequestFactory clientRequestFactory() {
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(15000);
        return factory;
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
                        "maxOutputTokens", 300
                )
        );

        try {
            Map<?, ?> response = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            List<?> candidates = (List<?>) response.get("candidates");
            Map<?, ?> firstCandidate = (Map<?, ?>) candidates.get(0);
            Map<?, ?> content = (Map<?, ?>) firstCandidate.get("content");
            List<?> parts = (List<?>) content.get("parts");
            Map<?, ?> firstPart = (Map<?, ?>) parts.get(0);
            return (String) firstPart.get("text");
       } catch (Exception e) {
    e.printStackTrace();
    return "ERROR: " + e.getMessage();
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