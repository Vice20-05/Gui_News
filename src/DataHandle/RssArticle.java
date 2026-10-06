package DataHandle;

// Holds the fields for one article parsed from an RSS feed.
public class RssArticle {
    private String title;
    private String description;
    private String url;
    private String publishDate;

    // Create an article using values read from one RSS item.
    public RssArticle(String title, String description, String url, String publishDate) {
        this.title = title;
        this.description = description;
        this.url = url;
        this.publishDate = publishDate;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getUrl() {
        return url;
    }

    public String getPublishDate() {
        return publishDate;
    }
}
