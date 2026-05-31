package com.frontendauction.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UserProfileModel {
    private String id;
    private String username;
    private Set<String> roles;
    @JsonAlias({"items", "itemList"})
    private List<String> itemList;
    @JsonAlias({"auctions", "auctionList"})
    private List<String> auctionList;
    private Boolean banned;
    private String status;

    public UserProfileModel() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public List<String> getItemList() {
        return itemList;
    }

    public void setItemList(List<String> itemList) {
        this.itemList = itemList;
    }

    public List<String> getAuctionList() {
        return auctionList;
    }

    public void setAuctionList(List<String> auctionList) {
        this.auctionList = auctionList;
    }

    public String getStatus() {
        if (status != null && !status.isBlank()) {
            return status;
        }
        return Boolean.TRUE.equals(banned) ? "BANNED" : "ACTIVE";
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getBanned() {
        return banned;
    }

    public void setBanned(Boolean banned) {
        this.banned = banned;
    }
}
