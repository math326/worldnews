package com.worldnews.app.model;

public class RssItem {
    private String title;
    private String description;
    private String link;
    private String pubDate;
    private String imageUrl;

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getLink() { return link; }
    public String getPubDate() { return pubDate; }
    public String getImageUrl() { return imageUrl; }

    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setLink(String link) { this.link = link; }
    public void setPubDate(String pubDate) { this.pubDate = pubDate; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    /** Converte para Article para reutilizar o NewsAdapter existente */
    public Article toArticle() {
        Article a = new Article();
        a.setTitle(title);
        a.setDescription(description);
        a.setUrl(link);
        a.setUrlToImage(imageUrl);
        a.setPublishedAt(pubDate);
        Article.Source source = new Article.Source();
        source.setName("G1");
        a.setSource(source);
        return a;
    }
}
