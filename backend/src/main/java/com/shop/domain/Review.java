package com.shop.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("reviews")
public class Review implements Persistable<String> {
    @Id
    private String id;
    private String reviewerUsername;
    private String targetUsername;
    private Integer stars;
    private String comment;
    private Instant createdAt;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override
    public boolean isNew() {
        return isNew;
    }
}
