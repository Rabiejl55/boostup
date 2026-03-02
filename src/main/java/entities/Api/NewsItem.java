package entities.Api;

public class NewsItem {
    private String title;
    private String link;
    private String source;
    private String pubDate;

    private String imageUrl; // ✅ NEW

    public NewsItem(String title, String link, String source, String pubDate) {
        this(title, link, source, pubDate, "");
    }

    public NewsItem(String title, String link, String source, String pubDate, String imageUrl) {
        this.title = title;
        this.link = link;
        this.source = source;
        this.pubDate = pubDate;
        this.imageUrl = (imageUrl == null) ? "" : imageUrl;
    }

    public String getTitle() { return title; }
    public String getLink() { return link; }
    public String getSource() { return source; }
    public String getPubDate() { return pubDate; }
    public String getImageUrl() { return imageUrl; }

    @Override
    public String toString() {
        return title; // garde simple (ListView custom cell affichera le reste)
    }

    public void setImageUrl(String imageUrl) {
    }
}