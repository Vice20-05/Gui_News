package Database;

import DataHandle.RssArticle;
import DataHandle.RssSource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

// Stores RSS sources and articles in a SQLite file (news.db).
//
// SQLite is a database that lives in a single file, so no server is needed.
// We talk to it with SQL through JDBC (Java's standard database API).
//
// Tables:
//   sources  -> one row per website from RSS.json
//   articles -> one row per news article, linked to its source by source_id
//
// "UNIQUE" on the url columns means the same website or article can never be stored twice.
// "INSERT OR IGNORE" then means: add the row if it is new, skip it if it already exists.
public class NewsDatabase {
    private final String connectionUrl;

    public NewsDatabase(String databaseFile) throws SQLException {
        this.connectionUrl = "jdbc:sqlite:" + databaseFile;
        createTables();
    }

    // Creates the tables the first time the program runs. Later runs do nothing here.
    private void createTables() throws SQLException {
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS sources (
                        id   INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL,
                        url  TEXT NOT NULL UNIQUE
                    )""");
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS articles (
                        id           INTEGER PRIMARY KEY AUTOINCREMENT,
                        source_id    INTEGER NOT NULL REFERENCES sources(id),
                        title        TEXT NOT NULL,
                        description  TEXT,
                        url          TEXT NOT NULL UNIQUE,
                        publish_date TEXT,
                        language     TEXT
                    )""");
        }
    }

    // Adds the website if it is not in the database yet, otherwise ignores it.
    // Returns the website's id either way, so articles can point to it.
    public int addSourceIfMissing(RssSource source) throws SQLException {
        try (Connection connection = connect()) {
            // "?" are placeholders. Using them (instead of gluing strings together)
            // protects against SQL injection.
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT OR IGNORE INTO sources (name, url) VALUES (?, ?)")) {
                insert.setString(1, source.getName());
                insert.setString(2, source.getUrl());
                insert.executeUpdate();
            }
            try (PreparedStatement select = connection.prepareStatement(
                    "SELECT id FROM sources WHERE url = ?")) {
                select.setString(1, source.getUrl());
                try (ResultSet result = select.executeQuery()) {
                    result.next();
                    return result.getInt("id");
                }
            }
        }
    }

    // Adds every article that is not stored yet. Returns how many were really new.
    public int addArticlesIfMissing(int sourceId, List<RssArticle> articles) throws SQLException {
        int added = 0;
        try (Connection connection = connect();
             PreparedStatement insert = connection.prepareStatement("""
                     INSERT OR IGNORE INTO articles
                         (source_id, title, description, url, publish_date, language)
                     VALUES (?, ?, ?, ?, ?, ?)""")) {
            for (RssArticle article : articles) {
                if (article.getUrl().isBlank()) {
                    continue; // Without a link we cannot tell duplicates apart, so skip it.
                }
                insert.setInt(1, sourceId);
                insert.setString(2, article.getTitle());
                insert.setString(3, article.getDescription());
                insert.setString(4, article.getUrl());
                insert.setString(5, article.getPublishDate());
                insert.setString(6, article.getLanguage());
                // executeUpdate returns 1 if the row was added, 0 if it was ignored.
                added += insert.executeUpdate();
            }
        }
        return added;
    }

    // Reads all stored articles (newest first) together with their website's name.
    public List<StoredArticle> loadArticles() throws SQLException {
        List<StoredArticle> articles = new ArrayList<>();
        // JOIN connects each article to its row in the sources table.
        String sql = """
                SELECT articles.title, articles.description, articles.url,
                       articles.publish_date, articles.language, sources.name AS source_name
                FROM articles
                JOIN sources ON sources.id = articles.source_id
                ORDER BY articles.id DESC""";
        try (Connection connection = connect();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            while (result.next()) {
                articles.add(new StoredArticle(
                        result.getString("source_name"),
                        new RssArticle(
                                result.getString("title"),
                                result.getString("description"),
                                result.getString("url"),
                                result.getString("publish_date"),
                                result.getString("language")
                        )
                ));
            }
        }
        return articles;
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(connectionUrl);
    }

    // An article read back from the database, plus the name of the website it came from.
    public record StoredArticle(String sourceName, RssArticle article) {
    }
}
