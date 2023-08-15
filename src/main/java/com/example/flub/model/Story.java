package com.example.flub.model;

import jakarta.persistence.*;

@Entity
public class Story {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Lob
    private String storyContext;
    @Lob
    private String prompt;
    @Lob
    private String storyText;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getStoryText() {
        return storyText;
    }

    public void setStoryText(String storyText) {
        this.storyText = storyText;
    }

    public String getStoryContext() {
        return storyContext;
    }

    public void setStoryContext(String storyContext) {
        this.storyContext = storyContext;
    }
}

