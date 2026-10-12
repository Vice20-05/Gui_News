package DataHandle;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Saves and loads RSS sources and articles in the local SQLite database (news.db). */
public class NewsDatabase {
    // The database is the news.db file in the folder the program is started from.
    private static final String DATABASE_URL = "jdbc:sqlite:news.db";

    // One connection that we open once and reuse for every query.
    // Opening a new connection for every single article was very slow.
    private final Connection connection;

    /** Opens the database file (creating it if needed) and makes sure the tables exist. */
    public NewsDatabase() throws SQLException {
        connection = DriverManager.getConnection(DATABASE_URL);
        createTables();

        // By default SQLite writes to the disk after EVERY insert, which is slow.
        // With auto-commit off, changes are collected and written all at once
        // when we call save().
        connection.setAutoCommit(false);
    }

    /** Writes all the changes made since the last save to the disk. */
    public void save() throws SQLException {
        connection.commit();
    }

    /** Deletes every article and source from the database. */
    public void clearAll() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM articles");
            statement.executeUpdate("DELETE FROM sources");
            save();
        } catch (SQLException e) {
            // Undo a half-finished delete, so we never end up with only one table cleared.
            connection.rollback();
            throw e;
        }
    }

    /** Creates the sources and articles tables the first time the program runs. */
    private void createTables() throws SQLException {
        // "IF NOT EXISTS" means nothing happens when the tables are already there.
        String sourcesTable = """
                CREATE TABLE IF NOT EXISTS sources (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    url TEXT NOT NULL UNIQUE
                )
                """;
        String articlesTable = """
                CREATE TABLE IF NOT EXISTS articles (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    source_id INTEGER NOT NULL,
                    source_name TEXT,
                    title TEXT,
                    description TEXT,
                    url TEXT NOT NULL UNIQUE,
                    publish_date TEXT,
                    language TEXT,
                    cover_image TEXT
                )
                """;

        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sourcesTable);
            statement.executeUpdate(articlesTable);
        }
        addCoverImageColumnIfMissing();
    }

    /** Adds the cover_image column to databases created before the column existed. */
    private void addCoverImageColumnIfMissing() throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet columns = statement.executeQuery("PRAGMA table_info(articles)")) {
            while (columns.next()) {
                if (columns.getString("name").equals("cover_image")) {
                    return;
                }
            }
        }
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("ALTER TABLE articles ADD COLUMN cover_image TEXT");
        }
    }

    /** Saves an RSS source if it is not saved yet, and returns its id. */
    public int addSourceIfMissing(String name, String url) throws SQLException {
        // "OR IGNORE" skips the insert when a source with this URL already exists.
        String insertSql = "INSERT OR IGNORE INTO sources (name, url) VALUES (?, ?)";
        String findIdSql = "SELECT id FROM sources WHERE url = ?";

        try (PreparedStatement insert = connection.prepareStatement(insertSql);
             PreparedStatement find = connection.prepareStatement(findIdSql)) {
            // The ? marks are filled in here, which keeps the SQL safe from odd characters.
            insert.setString(1, name);
            insert.setString(2, url);
            insert.executeUpdate();

            // Look up the id, both for a new source and for one that was already there.
            find.setString(1, url);
            try (ResultSet result = find.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("Could not find RSS source after saving it.");
                }
                return result.getInt("id");
            }
        }
    }

    /** Saves an article if its URL is not in the database yet. Returns true if it was new. */
    public boolean addArticleIfMissing(int sourceId, RssArticle article) throws SQLException {
        // An article without a link can't be checked for duplicates, so we skip it.
        if (article == null || article.getUrl().isEmpty()) {
            return false;
        }

        // The url column is UNIQUE, so "OR IGNORE" skips articles we already have.
        String sql = """
                INSERT OR IGNORE INTO articles
                (source_id, source_name, title, description, url, publish_date, language, cover_image)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, sourceId);
            statement.setString(2, article.getSourceName());
            statement.setString(3, article.getTitle());
            statement.setString(4, article.getDescription());
            statement.setString(5, article.getUrl());
            statement.setString(6, article.getPublishDate());
            statement.setString(7, article.getLanguage());
            statement.setString(8, article.getCoverImage());
            // executeUpdate returns how many rows were inserted: 1 if new, 0 if ignored.
            return statement.executeUpdate() == 1;
        }
    }

    /** Sets the language of this source's articles that don't have one yet. */
    public void fillMissingLanguage(String sourceName, String language) throws SQLException {
        if (language == null || language.isEmpty()) {
            return;
        }
        String sql = "UPDATE articles SET language = ? WHERE source_name = ? AND (language IS NULL OR language = '')";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, language);
            statement.setString(2, sourceName);
            statement.executeUpdate();
        }
    }

    /** Loads every saved article, newest first. */
    public List<RssArticle> loadArticles() throws SQLException {
        List<RssArticle> articles = new ArrayList<>();
        // A higher id means the article was saved later, so this shows the newest first.
        String sql = "SELECT * FROM articles ORDER BY id DESC";

        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(sql)) {
            // Turn every row of the result into an RssArticle object.
            while (rows.next()) {
                articles.add(new RssArticle(
                        rows.getString("title"),
                        rows.getString("description"),
                        rows.getString("url"),
                        rows.getString("publish_date"),
                        rows.getString("language"),
                        rows.getString("source_name"),
                        rows.getString("cover_image")
                ));
            }
        }
        return articles;
    }
}
