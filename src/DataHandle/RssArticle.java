package DataHandle;

// Holds the fields for one article parsed from an RSS feed.
public class RssArticle {
    private final String title;
    private final String description;
    private final String url;
    private final String publishDate;
    // Language key from the feed's <language> tag, for example "ro-RO" or "hu-HU".
    private final String language;

    // Create an article using values read from one RSS item.
    public RssArticle(String title, String description, String url, String publishDate) {
        this(title, description, url, publishDate, "");
    }

    public RssArticle(String title, String description, String url, String publishDate,
                      String language) {
        this.title = valueOrEmpty(title);
        this.description = valueOrEmpty(description);
        this.url = valueOrEmpty(url);
        this.publishDate = valueOrEmpty(publishDate);
        this.language = valueOrEmpty(language);
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

    public String getLanguage() {
        return language;
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
