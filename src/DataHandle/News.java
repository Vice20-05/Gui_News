package DataHandle;

/** Holds the fields for one article parsed from an RSS feed. */
public class RssArticle {
    // Used when an article has no cover image of its own.
    public static final String DEFAULT_COVER_IMAGE = "res/images/default.jpg";

    // The article data; final, because an article never changes after it is created.
    private final String title;
    private final String description;
    private final String url;
    private final String publishDate;
    private final String language ;
    private final String sourceName;
    // Local file path of the cover image, never empty.
    private final String coverImage;

    /** Creates an article that uses the default cover image. */
    public RssArticle(String title, String description,  String url, String publishDate, String language, String sourceName) {
        this(title, description, url, publishDate, language, sourceName, null);
    }

    /** Creates an article and replaces missing field values with empty strings. */
    public RssArticle(String title, String description,  String url, String publishDate, String language, String sourceName,
                      String coverImage) {
        // RSS feeds often contain HTML (<p>, &#8230;), so we keep only the plain text.
        this.title = HtmlText.toPlainText(title);
        this.description = HtmlText.toPlainText(description);
        this.url = valueOrEmpty(url);
        this.publishDate = valueOrEmpty(publishDate);
        this.language = valueOrEmpty(language);
        this.sourceName = valueOrEmpty(sourceName);
        if (coverImage == null || coverImage.isEmpty()) {
            coverImage = DEFAULT_COVER_IMAGE;
        }
        this.coverImage = coverImage;
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

    /** Returns the local path of the cover image. */
    public String getCoverImage() {
        return coverImage;
    }

    /** Returns an empty string instead of null, so the rest of the code never sees null. */
    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
