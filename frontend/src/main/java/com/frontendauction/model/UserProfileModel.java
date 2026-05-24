package com.frontendauction.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UserProfileModel {
    private String id;
    private String username;
    private Set<String> roles;
    private List<String> itemList;
    private List<String> auctionList;

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
}
