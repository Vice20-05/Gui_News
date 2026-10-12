package DataHandle;

/** Holds the name, URL and language of one RSS feed from the JSON configuration. */
public class RssSource {
    // Gson fills these fields from the JSON keys with the same names ("name", "url", "language").
    private String name;
    private String url;
    // "RO" or "HU".
    private String language;

    /** Returns the configured display name. */
    public String getName() {
        return name;
    }

    /** Returns the configured feed URL. */
    public String getUrl() {
        return url;
    }

    /** Returns the language of the feed, such as RO or HU. */
    public String getLanguage() {
        return language;
    }
}
