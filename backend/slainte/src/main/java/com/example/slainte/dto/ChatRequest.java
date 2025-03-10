package com.example.slainte.dto;

import java.util.List;

public class ChatRequest {
    private String prompt;
    private String model;
    private List<Message> messages;
    private Boolean useRag;  // Added useRag flag to control RAG usage

    // Default constructor (needed for Jackson deserialization)
    public ChatRequest() {}

    // Constructor for direct text input
    public ChatRequest(String prompt, String model) {
        this.prompt = prompt;
        this.model = model;
    }

    // Constructor for handling messages
    public ChatRequest(List<Message> messages) {
        this.messages = messages;
    }

    // Full constructor with all fields
    public ChatRequest(String prompt, String model, List<Message> messages, Boolean useRag) {
        this.prompt = prompt;
        this.model = model;
        this.messages = messages;
        this.useRag = useRag;
    }

    // Getter & Setter Methods
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

    public Boolean getUseRag() {
        return useRag;
    }

    public void setUseRag(Boolean useRag) {
        this.useRag = useRag;
    }
}