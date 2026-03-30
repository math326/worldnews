package com.worldnews.app.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "articles")
public class ArticleEntity {

    @PrimaryKey
    @NonNull
    private String url;

    private String title;
    private String description;
    private String urlToImage;
    private String publishedAt;
    private String sourceName;
    private String category;
    private long savedAt;

    public ArticleEntity(@NonNull String url, String title, String description,
                         String urlToImage, String publishedAt, String sourceName,
                         String category) {
        this.url = url;
        this.title = title;
        this.description = description;
        this.urlToImage = urlToImage;
        this.publishedAt = publishedAt;
        this.sourceName = sourceName;
        this.category = category;
        this.savedAt = System.currentTimeMillis();
    }

    @NonNull
    public String getUrl() { return url; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getUrlToImage() { return urlToImage; }
    public String getPublishedAt() { return publishedAt; }
    public String getSourceName() { return sourceName; }
    public String getCategory() { return category; }
    public long getSavedAt() { return savedAt; }

    public void setUrl(@NonNull String url) { this.url = url; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setUrlToImage(String urlToImage) { this.urlToImage = urlToImage; }
    public void setPublishedAt(String publishedAt) { this.publishedAt = publishedAt; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }
    public void setCategory(String category) { this.category = category; }
    public void setSavedAt(long savedAt) { this.savedAt = savedAt; }
}
