package com.example.slainte.dto;

import java.util.List;

public class ChatRequest {
    private String prompt;
    private String model;
    private List<Message> messages;  // ✅ Add support for multiple messages

    // ✅ Default constructor (needed for Jackson deserialization)
    public ChatRequest() {}

    // ✅ Constructor for direct text input
    public ChatRequest(String prompt, String model) {
        this.prompt = prompt;
        this.model = model;
    }

    // ✅ Constructor for handling messages
    public ChatRequest(List<Message> messages) {
        this.messages = messages;
    }

    // ✅ Getter & Setter Methods
    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }
}
