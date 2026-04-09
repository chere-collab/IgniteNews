package com.example.tryanderror;

import com.google.gson.annotations.SerializedName;

public class Source {
    @SerializedName("name")
    private String name;

    public Source() {}

    public Source(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
