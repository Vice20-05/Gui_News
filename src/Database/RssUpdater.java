package Database;

import DataHandle.JsonReader;
import DataHandle.RssArticle;
import DataHandle.RssSource;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

// Connects the three parts of the program:
//   RSS.json  --(JsonReader)-->  list of websites
//   website   --(RssXmlParser)--> list of articles (downloaded over HTTPS)
//   articles  --(NewsDatabase)--> saved in SQLite, duplicates ignored
public class RssUpdater {
    private final JsonReader jsonReader = new JsonReader();
    private final RssXmlParser parser = new RssXmlParser();
    private final NewsDatabase database;

    public RssUpdater(NewsDatabase database) {
        this.database = database;
    }

    public void updateAll(String jsonPath) throws IOException, SQLException {
        List<RssSource> sources = jsonReader.readSources(jsonPath);

        for (RssSource source : sources) {
            int sourceId = database.addSourceIfMissing(source);
            try {
                List<RssArticle> articles = parser.fetchArticles(source.getUrl());
                int added = database.addArticlesIfMissing(sourceId, articles);
                System.out.println(source.getName() + ": " + added + " new articles");
            } catch (IOException e) {
                // One broken website should not stop the others from loading.
                System.err.println("Skipping " + source.getName() + ": " + e.getMessage());
            }
        }
    }
}
