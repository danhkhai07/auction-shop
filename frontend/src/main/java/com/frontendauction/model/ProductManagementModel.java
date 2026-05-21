package com.frontendauction.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductManagementModel {
    private String id;
    private String name;
    private String description;
    private Double startingPrice;
    private String status;
    private String sellerId;

    public ProductManagementModel() {}

    public ProductManagementModel(String id, String name, String description, Double startingPrice) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getStartingPrice() { return startingPrice; }
    public void setStartingPrice(Double startingPrice) { this.startingPrice = startingPrice; }

    public Double getCurrentPrice() { return startingPrice; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }
}
