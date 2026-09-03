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
    private static final int MAX_HISTORY_TURNS = 10;
    private static final String FALLBACK_MESSAGE =
            "Sorry, I'm getting a lot of questions right now — please try again in a few seconds.";
    private static final String PARSE_FALLBACK_MESSAGE =
            "I couldn't quite generate a response to that. Could you rephrase your question?";

    private final ProductRepository productRepository;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.model:gemini-3.6-flash}")
    private String geminiModel;

    private final java.net.http.HttpClient httpClient = java.net.http.HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final com.fasterxml.jackson.databind.ObjectMapper mapper =
            new com.fasterxml.jackson.databind.ObjectMapper();

    public AssistantService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public String getAnswer(AssistantRequest request) {
        List<Product> products = productRepository.findAll();
        if (products.size() > MAX_PRODUCTS_IN_CONTEXT) {
            products = products.subList(0, MAX_PRODUCTS_IN_CONTEXT);
        }

        String catalog = products.isEmpty()
                ? "(No products currently in stock.)"
                : products.stream()
                    .map(p -> String.format("- %s ($%.2f) | %s | %s",
                            p.getName(), p.getPrice(), p.getCategory(), p.getDescription()))
                    .collect(Collectors.joining("\n"));

        String systemInstruction =
                "You are ShopZone's shopping assistant — knowledgeable, friendly, and genuinely helpful, like a good in-store sales associate.\n\n" +
                "WHAT YOU CAN DO:\n" +
                "- Recommend products from the catalog below based on price, category, or description\n" +
                "- Compare products (e.g. price, features) when asked\n" +
                "- Filter and sort mentally (e.g. \"cheapest first\", \"under $X\", \"in category Y\")\n" +
                "- Answer general questions about what's available or in stock\n" +
                "- Have a natural, multi-turn conversation — remember what was discussed earlier in this chat\n\n" +
                "WHAT YOU CANNOT DO (be upfront and helpful about this, don't just refuse):\n" +
                "- You cannot place orders, add items to a cart, or process payments yourself. " +
                "If asked to do this, explain that you can help them find the right product, " +
                "but they need to click \"Add to Cart\" on the product page themselves to complete the purchase.\n" +
                "- You only know about the products listed below — you have no access to order history, account details, or shipping info.\n\n" +
                "STYLE:\n" +
                "- Be concise but complete — a few sentences is fine, don't ramble, but don't cut yourself short either\n" +
                "- Always mention exact product names and prices when recommending something\n" +
                "- If nothing in the catalog matches, say so honestly and suggest what IS available instead\n" +
                "- If a question is ambiguous, ask a brief clarifying question rather than guessing\n\n" +
                "CATALOG:\n" + catalog;

        List<Map<String, Object>> contents = buildConversation(request, systemInstruction);

        Map<String, Object> requestBody = Map.of(
                "contents", contents,
                "generationConfig", Map.of(
                        "temperature", 0.5,
                        "maxOutputTokens", 1024
                )
        );

        String jsonBody;
        try {
            jsonBody = mapper.writeValueAsString(requestBody);
        } catch (Exception e) {
            return FALLBACK_MESSAGE;
        }

        // Try up to 2 times total, in case of a transient failure (e.g. brief rate limit or network blip)
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                String result = callGemini(jsonBody);
                if (result != null) {
                    return result;
                }
            } catch (Exception ignored) {
                // fall through to retry / final fallback
            }
            if (attempt == 1) {
                try {
                    Thread.sleep(800);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        return FALLBACK_MESSAGE;
    }

    /**
     * Calls Gemini once. Returns the answer text, PARSE_FALLBACK_MESSAGE if the response
     * shape was unexpected, or null if the call itself failed (caller may retry).
     */
    private String callGemini(String jsonBody) throws Exception {
        String url = String.format(
                "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
                geminiModel, geminiApiKey);

        java.net.http.HttpRequest httpRequest = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(15))
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        java.net.http.HttpResponse<String> httpResponse =
                httpClient.send(httpRequest, java.net.http.HttpResponse.BodyHandlers.ofString());

        if (httpResponse.statusCode() != 200) {
            // Signal "try again" for transient errors (429 rate limit, 5xx server issues)
            return null;
        }

        Map<?, ?> response = mapper.readValue(httpResponse.body(), Map.class);

        List<?> candidates = (List<?>) response.get("candidates");
        if (candidates == null || candidates.isEmpty()) {
            return PARSE_FALLBACK_MESSAGE;
        }
        Map<?, ?> firstCandidate = (Map<?, ?>) candidates.get(0);

        Map<?, ?> content = (Map<?, ?>) firstCandidate.get("content");
        if (content == null) {
            return PARSE_FALLBACK_MESSAGE;
        }
        List<?> parts = (List<?>) content.get("parts");
        if (parts == null || parts.isEmpty()) {
            return PARSE_FALLBACK_MESSAGE;
        }
        Map<?, ?> firstPart = (Map<?, ?>) parts.get(0);
        Object text = firstPart.get("text");
        return text == null ? PARSE_FALLBACK_MESSAGE : text.toString();
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