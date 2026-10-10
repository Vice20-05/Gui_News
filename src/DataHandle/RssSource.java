package DataHandle;

/** Holds the name and URL of one RSS feed from the JSON configuration. */
public class RssSource {
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
