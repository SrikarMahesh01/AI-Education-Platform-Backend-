package com.example.demo.model;

public class Message {
    private String speaker;
    private String message;

    public Message() {}

    public Message(String speaker, String message) {
        this.speaker = speaker;
        this.message = message;
    }

    public String getSpeaker() {
        return speaker;
    }

    public void setSpeaker(String speaker) {
        this.speaker = speaker;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
