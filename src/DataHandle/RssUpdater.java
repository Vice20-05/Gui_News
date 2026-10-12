package DataHandle;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Downloads configured feeds and stores new news items in the database. */
public class RssUpdater {
    /**
     * Updates every configured RSS source, continuing when one source fails.
     * Returns one message for every source that could not be loaded (empty if all worked).
     */
    public static List<String> updateAll(String jsonPath, NewsDatabase database) throws IOException, SQLException {
        JsonReader jsonReader = new JsonReader();
        RssXmlParser parser = new RssXmlParser();
        List<String> problems = new ArrayList<>();

        // Read all configured websites.
        List<RssSource> sources = jsonReader.readSources(jsonPath);

        // Download and store news for each website.
        for (RssSource source : sources) {
            // Step 1: ask the server for the status code before downloading anything.
            String feedUrl = source.getUrl();
            int status = HttpHelper.getStatusCode(feedUrl);

            // Step 2: on a redirect (301/302), follow the new address once.
            if (HttpHelper.isRedirect(status)) {
                String newUrl = HttpHelper.getRedirectLocation(feedUrl);
                System.out.println(source.getName() + ": " + HttpHelper.describeStatus(status) + " -> " + newUrl);
                if (newUrl != null) {
                    feedUrl = newUrl;
                    status = HttpHelper.getStatusCode(feedUrl);
                }
            }

            // Step 3: only download and parse the feed when the server answered 200 OK.
            if (status != 200) {
                String message = source.getName() + ": " + HttpHelper.describeStatus(status);
                System.out.println("Skipped " + message);
                problems.add(message);
                continue;
            }

            try {
                // Download the news for this source.
                List<News> newsList = parser.fetchNews(feedUrl, source.getName(), source.getLanguage());

                // Save each item; addNews() ignores the ones we already have.
                int newCount = 0;
                for (News news : newsList) {
                    if (database.addNews(news)) {
                        newCount++;
                    }
                }
                System.out.println(source.getName() + ": " + newCount + " new articles");

            } catch (IOException e) {
                // If one website is down, we print a message and continue with the next one.
                // Without this try/catch, one broken website would stop everything.
                System.out.println("Skipped " + source.getName() + ": " + e.getMessage());
                problems.add(source.getName() + ": " + e.getMessage());
            }

            // Write this website's changes to the disk in one go (much faster).
            database.save();
        }
        return problems;
    }
}
