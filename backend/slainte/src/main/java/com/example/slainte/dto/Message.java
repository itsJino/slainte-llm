package com.example.slainte.dto;

public class Message {
    private String role;
    private String content;

    // ✅ Default constructor (needed for Jackson)
    public Message() {}

    // ✅ Constructor with parameters
    public Message(String role, String content) {
        this.role = role;
        this.content = content;
    }

    // ✅ Getter & Setter Methods
    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
