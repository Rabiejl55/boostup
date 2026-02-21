package entities.Api;

public class NewsItem {
    private final String title;
    private final String link;
    private final String pubDate;
    private final String source;

    public NewsItem(String title, String link, String pubDate, String source) {
        this.title = title;
        this.link = link;
        this.pubDate = pubDate;
        this.source = source;
    }

    public String getTitle() { return title; }
    public String getLink() { return link; }
    public String getPubDate() { return pubDate; }
    public String getSource() { return source; }

    @Override
    public String toString() {
        // ce qui s'affiche dans la ListView
        return title + (source != null && !source.isBlank() ? " — " + source : "");
    }
}