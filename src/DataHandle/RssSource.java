package DataHandle;

/** Holds the name and URL of one RSS feed from the JSON configuration. */
public class RssSource {
    // Gson fills these fields from the JSON keys with the same names ("name", "url").
    private String name;
    private String url;

    /** Returns the configured display name. */
    public String getName() {
        return name;
    }

    /** Returns the configured feed URL. */
    public String getUrl() {
        return url;
    }
}
