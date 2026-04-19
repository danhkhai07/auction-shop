package com.shop.domain;

public class Item {
    private String id;
    private String name;
    private String description;
    private User seller;

    public Item(String id, String name, String description, User seller) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.seller = seller;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public User getSeller() {
        return seller;
    }

    public void setSeller(User seller) {
        this.seller = seller;
    }
}