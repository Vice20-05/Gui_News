package DataHandle;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class RssUpdater {
    public static void updateAll(String jsonPath, NewsDatabase database) throws IOException, SQLException {
        JsonReader jsonReader = new JsonReader();
        RssXmlParser parser = new RssXmlParser();

        // 1. Read all the websites from RSS.json.
        List<RssSource> sources = jsonReader.readSources(jsonPath);

        // 2. Do the same steps for every website.
        for (RssSource source : sources) {
            // 2a. Save the website (skipped if it is already saved) and get its id.
            int sourceId = database.addSourceIfMissing(source.getName(), source.getUrl());

            try {
                // 2b. Download the articles of this website.
                List<RssArticle> articles = parser.fetchArticales(source);

                // 2c. Save each article and count how many were new.
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
