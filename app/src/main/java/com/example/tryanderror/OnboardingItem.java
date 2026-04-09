package com.example.tryanderror;

public class OnboardingItem {
    private String title;
    private String description;
    private int imageRes;

    public OnboardingItem(String title, String description, int imageRes) {
        this.title = title;
        this.description = description;
        this.imageRes = imageRes;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public int getImageRes() { return imageRes; }
}
