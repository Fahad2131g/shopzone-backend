package com.shopzone.product_service.dto;

import java.util.List;

public class AssistantRequest {
    private String message;
    private List<ChatTurn> history;

    public static class ChatTurn {
        private String role;
        private String text;

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public List<ChatTurn> getHistory() { return history; }
    public void setHistory(List<ChatTurn> history) { this.history = history; }
}