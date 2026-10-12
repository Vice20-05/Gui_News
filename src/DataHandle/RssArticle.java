package DataHandle;

/** Holds the fields for one article parsed from an RSS feed. */
public class RssArticle {
    private final String title;
    private final String description;
    private final String url;
    private final String publishDate;
    private final String language ;
    private final String sourceName;

    /** Creates an article and replaces missing field values with empty strings. */
    public RssArticle(String title, String description,  String url, String publishDate, String language, String sourceName) {
        this.title = valueOrEmpty(title);
        this.description = valueOrEmpty(description);
        this.url = valueOrEmpty(url);
        this.publishDate = valueOrEmpty(publishDate);
        this.language = valueOrEmpty(language);
        this.sourceName = valueOrEmpty(sourceName);
    }

    /** Returns the article title. */
    public String getTitle() {
        return title;
    }

    /** Returns the article description. */
    public String getDescription() {
        return description;
    }

    /** Returns the article URL. */
    public String getUrl() {
        return url;
    }

    /** Returns the publication date. */
    public String getPublishDate() {
        return publishDate;
    }

    /** Returns the article language. */
    public String getLanguage() {
        return language;
    }

    /** Returns the name of the source feed. */
    public String getSourceName() {
        return sourceName;
    }


    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
