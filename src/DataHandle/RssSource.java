package DataHandle;

// Holds the name and URL of one RSS feed from the JSON configuration.
public class RssSource {
    private String name;
    private String url;

    // Gson fills these fields when it reads an RSS source from JSON.
    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }
}
