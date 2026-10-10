package DataHandle;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class NewsDatabase {
    private static final String DATABASE_URL = "jdbc:sqlite:news.db";

    public NewsDatabase() throws SQLException {
        createTables();
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(DATABASE_URL);
    }

    private void createTables() throws SQLException {
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
                    language TEXT
                )
                """;

        try (Connection connection = connect();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(sourcesTable);
            statement.executeUpdate(articlesTable);
        }
    }

    public int addSourceIfMissing(String name, String url) throws SQLException {
        String insertSql = "INSERT OR IGNORE INTO sources (name, url) VALUES (?, ?)";
        String findIdSql = "SELECT id FROM sources WHERE url = ?";

        try (Connection connection = connect();
             PreparedStatement insert = connection.prepareStatement(insertSql);
             PreparedStatement find = connection.prepareStatement(findIdSql)) {
            insert.setString(1, name);
            insert.setString(2, url);
            insert.executeUpdate();

            find.setString(1, url);
            try (ResultSet result = find.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("Could not find RSS source after saving it.");
                }
                return result.getInt("id");
            }
        }
    }

    public boolean addArticleIfMissing(int sourceId, RssArticle article) throws SQLException {
        if (article == null || article.getUrl().isEmpty()) {
            return false;
        }

        String sql = """
                INSERT OR IGNORE INTO articles
                (source_id, source_name, title, description, url, publish_date, language)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, sourceId);
            statement.setString(2, article.getSourceName());
            statement.setString(3, article.getTitle());
            statement.setString(4, article.getDescription());
            statement.setString(5, article.getUrl());
            statement.setString(6, article.getPublishDate());
            statement.setString(7, article.getLanguage());
            return statement.executeUpdate() == 1;
        }
    }

    public List<RssArticle> loadArticles() throws SQLException {
        List<RssArticle> articles = new ArrayList<>();
        String sql = "SELECT * FROM articles ORDER BY id DESC";

        try (Connection connection = connect();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(sql)) {
            while (rows.next()) {
                articles.add(new RssArticle(
                        rows.getString("title"),
                        rows.getString("description"),
                        rows.getString("url"),
                        rows.getString("publish_date"),
                        rows.getString("language"),
                        rows.getString("source_name")
                ));
            }
        }
        return articles;
    }
}
