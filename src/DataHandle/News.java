package DataHandle;

import java.sql.Timestamp;

/** One news item: either downloaded from an RSS feed or added by the user. */
public class News {
    // Used when a news item has no cover image of its own.
    public static final String DEFAULT_COVER_IMAGE = "res/images/default.jpg";

    private String title;
    private String description;
    // source + url together are the primary key in the database.
    private String source;
    private String author;
    private String publisher;
    private Timestamp publishDate;
    private String url;
    // When the item was saved to the database and when it was last changed.
    private Timestamp creationDate;
    private Timestamp lastModifiedDate;
    // "RO" or "HU".
    private String language;
    // Local file path of the cover image.
    private String coverImage;

    /** Creates a news item; missing text values become "" and a missing cover becomes the default image. */
    public News(String title, String description, String source, String author, String publisher,
                Timestamp publishDate, String url, Timestamp creationDate, Timestamp lastModifiedDate,
                String language, String coverImage) {
        this.title = valueOrEmpty(title);
        this.description = valueOrEmpty(description);
        this.source = valueOrEmpty(source);
        this.author = valueOrEmpty(author);
        this.publisher = valueOrEmpty(publisher);
        this.publishDate = publishDate;
        this.url = valueOrEmpty(url);
        this.creationDate = creationDate;
        this.lastModifiedDate = lastModifiedDate;
        this.language = valueOrEmpty(language);
        setCoverImage(coverImage);
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = valueOrEmpty(title); }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = valueOrEmpty(description); }

    public String getSource() { return source; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = valueOrEmpty(author); }

    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = valueOrEmpty(publisher); }

    public Timestamp getPublishDate() { return publishDate; }
    public void setPublishDate(Timestamp publishDate) { this.publishDate = publishDate; }

    public String getUrl() { return url; }

    public Timestamp getCreationDate() { return creationDate; }
    public void setCreationDate(Timestamp creationDate) { this.creationDate = creationDate; }

    public Timestamp getLastModifiedDate() { return lastModifiedDate; }
    public void setLastModifiedDate(Timestamp lastModifiedDate) { this.lastModifiedDate = lastModifiedDate; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = valueOrEmpty(language); }

    public String getCoverImage() { return coverImage; }

    /** Sets the cover image, falling back to the default image when it is empty. */
    public void setCoverImage(String coverImage) {
        if (coverImage == null || coverImage.isBlank()) {
            coverImage = DEFAULT_COVER_IMAGE;
        }
        this.coverImage = coverImage;
    }

    // Source and url have no setters: they form the primary key, so they never change.

    /** The text shown for this item in the list on the left. */
    @Override
    public String toString() {
        return title;
    }

    /** Returns an empty string instead of null, so the rest of the code never sees null. */
    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
