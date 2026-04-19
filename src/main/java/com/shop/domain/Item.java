package com.shop.domain;

public class Item {
    private final String id;
    private String name;
    private String description;
    private final User seller;

    public Item(String id, String name, String description, User seller) {
        if (id == null)
            throw new IllegalArgumentException("ID sản phẩm không được để trống.");
        if (name == null)
            throw new IllegalArgumentException("Tên sản phẩm không được để trống.");
        if (seller == null)
            throw new IllegalArgumentException("Phải có người bán.");
        this.id = id;
        this.name = name;
        this.description = description;
        this.seller = seller;
    }

    public String getId() {
        return id;
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

    public boolean isOwnedBy(User user) {
        if (user == null || this.seller == null) return false;
        return this.seller.getId().equals(user.getId());
    }
}