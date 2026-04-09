package com.example.tryanderror;

public class Category {
    private String name;
    private String key;
    private String emoji;
    private String colorHex;

    public Category(String name, String key, String emoji, String colorHex) {
        this.name = name;
        this.key = key;
        this.emoji = emoji;
        this.colorHex = colorHex;
    }

    public String getName() { return name; }
    public String getKey() { return key; }
    public String getEmoji() { return emoji; }
    public String getColorHex() { return colorHex; }
}
