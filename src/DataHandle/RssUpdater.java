package DataHandle;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/** Downloads configured feeds and stores new articles in the database. */
public class RssUpdater {
    /** Updates every configured RSS source, continuing when one source fails. */
    public static void updateAll(String jsonPath, NewsDatabase database) throws IOException, SQLException {
        JsonReader jsonReader = new JsonReader();
        RssXmlParser parser = new RssXmlParser();

        // Read all configured websites.
        List<RssSource> sources = jsonReader.readSources(jsonPath);

        // Download and store articles for each website.
        for (RssSource source : sources) {
            // Save the source and get its database id.
            int sourceId = database.addSourceIfMissing(source.getName(), source.getUrl());

            try {
                // Download the articles for this source.
                List<RssArticle> articles = parser.fetchArticles(source);

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
            }
        }
    }
}
