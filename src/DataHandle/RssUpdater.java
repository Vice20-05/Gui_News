package DataHandle;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Downloads configured feeds and stores new articles in the database. */
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

        // Download and store articles for each website.
        for (RssSource source : sources) {
            // Save the source and get its database id.
            int sourceId = database.addSourceIfMissing(source.getName(), source.getUrl());
            database.fillMissingLanguage(source.getName(), source.getLanguage());

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
                // Download the articles for this source.
                List<RssArticle> articles = parser.fetchArticles(feedUrl, source.getName(), source.getLanguage());

                // Save each article and count newly inserted records.
                int newCount = 0;
                for (RssArticle article : articles) {
                    if (database.addArticleIfMissing(sourceId, article)) {
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

    /** Sets the language of saved articles that were stored before sources had a language. */
    public static void fillMissingLanguages(String jsonPath, NewsDatabase database) throws IOException, SQLException {
        for (RssSource source : new JsonReader().readSources(jsonPath)) {
            database.fillMissingLanguage(source.getName(), source.getLanguage());
        }
        database.save();
    }
}
