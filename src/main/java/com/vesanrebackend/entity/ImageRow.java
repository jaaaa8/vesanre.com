package com.vesanrebackend.entity;

import java.util.UUID;

// Common shape of venue_images / court_images (Lombok getters/setters implement it), so one service serves both.
public interface ImageRow {
    UUID getId();

    String getStorageKey();

    void setStorageKey(String storageKey);

    String getAltText();

    void setAltText(String altText);

    Integer getSortOrder();

    void setSortOrder(Integer sortOrder);

    Boolean getIsCover();

    void setIsCover(Boolean isCover);
}
