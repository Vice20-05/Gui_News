package DataHandle;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/** All database work: creates the news and events tables and does Add, Read, Update and Delete on them. */
public class NewsDatabase {
    // The database is the news.db file in the folder the program is started from.
    private static final String DATABASE_URL = "jdbc:sqlite:news.db";

    // One connection that we open once and reuse for every query.
    // Opening a new connection for every single article was very slow.
    private final Connection connection;

    /** Opens the database file (creating it if needed) and makes sure the table exists. */
    public NewsDatabase() throws SQLException {
        connection = DriverManager.getConnection(DATABASE_URL);
        createTable();

        // By default SQLite writes to the disk after EVERY insert, which is slow.
        // With auto-commit off, changes are collected and written all at once
        // when we call save().
        connection.setAutoCommit(false);
    }

    /** Writes all the changes made since the last save to the disk. */
    public void save() throws SQLException {
        connection.commit();
    }

    /** Creates the news and events tables the first time the program runs. */
    private void createTable() throws SQLException {
        // The primary key is made of two columns: source + url.
        // The same article (same url from the same source) can only be saved once,
        // so this also stops duplicates when we refresh the feeds.
        String newsTable = """
                CREATE TABLE IF NOT EXISTS news (
                    title TEXT NOT NULL,
                    description TEXT,
                    source TEXT NOT NULL,
                    author TEXT,
                    publisher TEXT,
                    publish_date TIMESTAMP,
                    url TEXT NOT NULL,
                    creation_date TIMESTAMP NOT NULL,
                    last_modified_date TIMESTAMP NOT NULL,
                    language TEXT NOT NULL,
                    cover_image TEXT,
                    PRIMARY KEY (source, url)
                )
                """;
        // Events have the same columns as news, plus when and where they happen.
        // Their primary key is title + event_datetime + location.
        String eventsTable = """
                CREATE TABLE IF NOT EXISTS events (
                    title TEXT NOT NULL,
                    description TEXT,
                    source TEXT,
                    author TEXT,
                    publisher TEXT,
                    publish_date TIMESTAMP,
                    url TEXT,
                    creation_date TIMESTAMP NOT NULL,
                    last_modified_date TIMESTAMP NOT NULL,
                    language TEXT NOT NULL,
                    cover_image TEXT,
                    event_datetime TIMESTAMP NOT NULL,
                    location TEXT NOT NULL,
                    PRIMARY KEY (title, event_datetime, location)
                )
                """;
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(newsTable);
            statement.executeUpdate(eventsTable);
        }
    }

    /** CREATE: saves a news item if it is not saved yet. Returns true if it was new. */
    public boolean addNews(News news) throws SQLException {
        // "OR IGNORE" skips the insert when the (source, url) key already exists.
        String sql = """
                INSERT OR IGNORE INTO news
                (title, description, source, author, publisher, publish_date, url,
                 creation_date, last_modified_date, language, cover_image)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        Timestamp now = now();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            // The ? marks are filled in here, which keeps the SQL safe from odd characters.
            statement.setString(1, news.getTitle());
            statement.setString(2, news.getDescription());
            statement.setString(3, news.getSource());
            statement.setString(4, news.getAuthor());
            statement.setString(5, news.getPublisher());
            statement.setString(6, toText(news.getPublishDate()));
            statement.setString(7, news.getUrl());
            statement.setString(8, toText(now));
            statement.setString(9, toText(now));
            statement.setString(10, news.getLanguage());
            statement.setString(11, news.getCoverImage());
            // executeUpdate returns how many rows were inserted: 1 if new, 0 if ignored.
            return statement.executeUpdate() == 1;
        }
    }

    /** READ: returns one news item by its key, or null if it doesn't exist. */
    public News getNews(String source, String url) throws SQLException {
        String sql = "SELECT * FROM news WHERE source = ? AND url = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, source);
            statement.setString(2, url);
            try (ResultSet row = statement.executeQuery()) {
                return row.next() ? readNews(row) : null;
            }
        }
    }

    /** READ: returns every saved news item, newest first. */
    public List<News> getAllNews() throws SQLException {
        List<News> newsList = new ArrayList<>();
        String sql = "SELECT * FROM news ORDER BY publish_date DESC";
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(sql)) {
            // Turn every row of the result into a News object.
            while (rows.next()) {
                newsList.add(readNews(rows));
            }
        }
        return newsList;
    }

    /** UPDATE: saves the changed fields of a news item. Source and url (the key) stay the same. */
    public void updateNews(News news) throws SQLException {
        String sql = """
                UPDATE news SET title = ?, description = ?, author = ?, publisher = ?,
                    publish_date = ?, last_modified_date = ?, language = ?, cover_image = ?
                WHERE source = ? AND url = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, news.getTitle());
            statement.setString(2, news.getDescription());
            statement.setString(3, news.getAuthor());
            statement.setString(4, news.getPublisher());
            statement.setString(5, toText(news.getPublishDate()));
            statement.setString(6, toText(now()));
            statement.setString(7, news.getLanguage());
            statement.setString(8, news.getCoverImage());
            statement.setString(9, news.getSource());
            statement.setString(10, news.getUrl());
            statement.executeUpdate();
        }
    }

    /** DELETE: removes one news item by its key. */
    public void deleteNews(String source, String url) throws SQLException {
        String sql = "DELETE FROM news WHERE source = ? AND url = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, source);
            statement.setString(2, url);
            statement.executeUpdate();
        }
    }

    /** CREATE: saves an event if it is not saved yet. Returns true if it was new. */
    public boolean addEvent(Event event) throws SQLException {
        // "OR IGNORE" skips the insert when the (title, event_datetime, location) key already exists.
        String sql = """
                INSERT OR IGNORE INTO events
                (title, description, source, author, publisher, publish_date, url,
                 creation_date, last_modified_date, language, cover_image, event_datetime, location)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        Timestamp now = now();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, event.getTitle());
            statement.setString(2, event.getDescription());
            statement.setString(3, event.getSource());
            statement.setString(4, event.getAuthor());
            statement.setString(5, event.getPublisher());
            statement.setString(6, toText(event.getPublishDate()));
            statement.setString(7, event.getUrl());
            statement.setString(8, toText(now));
            statement.setString(9, toText(now));
            statement.setString(10, event.getLanguage());
            statement.setString(11, event.getCoverImage());
            statement.setString(12, toText(event.getEventDateTime()));
            statement.setString(13, event.getLocation());
            return statement.executeUpdate() == 1;
        }
    }

    /** READ: returns one event by its key, or null if it doesn't exist. */
    public Event getEvent(String title, Timestamp eventDateTime, String location) throws SQLException {
        String sql = "SELECT * FROM events WHERE title = ? AND event_datetime = ? AND location = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, title);
            statement.setString(2, toText(eventDateTime));
            statement.setString(3, location);
            try (ResultSet row = statement.executeQuery()) {
                return row.next() ? readEvent(row) : null;
            }
        }
    }

    /** READ: returns every saved event, the one that starts first at the top. */
    public List<Event> getAllEvents() throws SQLException {
        List<Event> events = new ArrayList<>();
        String sql = "SELECT * FROM events ORDER BY event_datetime";
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(sql)) {
            while (rows.next()) {
                events.add(readEvent(rows));
            }
        }
        return events;
    }

    /**
     * UPDATE: replaces the saved event "original" with "changed".
     * The old key is needed in the WHERE part, because title, time and location can all be edited.
     */
    public void updateEvent(Event original, Event changed) throws SQLException {
        String sql = """
                UPDATE events SET title = ?, description = ?, source = ?, author = ?, publisher = ?,
                    publish_date = ?, url = ?, last_modified_date = ?, language = ?, cover_image = ?,
                    event_datetime = ?, location = ?
                WHERE title = ? AND event_datetime = ? AND location = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, changed.getTitle());
            statement.setString(2, changed.getDescription());
            statement.setString(3, changed.getSource());
            statement.setString(4, changed.getAuthor());
            statement.setString(5, changed.getPublisher());
            statement.setString(6, toText(changed.getPublishDate()));
            statement.setString(7, changed.getUrl());
            statement.setString(8, toText(now()));
            statement.setString(9, changed.getLanguage());
            statement.setString(10, changed.getCoverImage());
            statement.setString(11, toText(changed.getEventDateTime()));
            statement.setString(12, changed.getLocation());
            // The old key finds the row to change.
            statement.setString(13, original.getTitle());
            statement.setString(14, toText(original.getEventDateTime()));
            statement.setString(15, original.getLocation());
            statement.executeUpdate();
        }
    }

    /** DELETE: removes one event by its key. */
    public void deleteEvent(String title, Timestamp eventDateTime, String location) throws SQLException {
        String sql = "DELETE FROM events WHERE title = ? AND event_datetime = ? AND location = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, title);
            statement.setString(2, toText(eventDateTime));
            statement.setString(3, location);
            statement.executeUpdate();
        }
    }

    /** Deletes every news item and every event from the database. */
    public void clearAll() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM news");
            statement.executeUpdate("DELETE FROM events");
            save();
        }
    }

    /** Builds a News object from the current row of a query result. */
    private News readNews(ResultSet row) throws SQLException {
        return new News(
                row.getString("title"),
                row.getString("description"),
                row.getString("source"),
                row.getString("author"),
                row.getString("publisher"),
                toTimestamp(row.getString("publish_date")),
                row.getString("url"),
                toTimestamp(row.getString("creation_date")),
                toTimestamp(row.getString("last_modified_date")),
                row.getString("language"),
                row.getString("cover_image")
        );
    }

    /** Builds an Event object from the current row of a query result. */
    private Event readEvent(ResultSet row) throws SQLException {
        return new Event(
                row.getString("title"),
                row.getString("description"),
                row.getString("source"),
                row.getString("author"),
                row.getString("publisher"),
                toTimestamp(row.getString("publish_date")),
                row.getString("url"),
                toTimestamp(row.getString("creation_date")),
                toTimestamp(row.getString("last_modified_date")),
                row.getString("language"),
                row.getString("cover_image"),
                toTimestamp(row.getString("event_datetime")),
                row.getString("location")
        );
    }

    // Timestamps are stored as readable text like "2026-10-20 18:30:00.0",
    // which also sorts correctly with ORDER BY.

    /** Turns a Timestamp into text for the database (null stays null). */
    private static String toText(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toString();
    }

    /** Turns database text back into a Timestamp (null or bad text gives null). */
    private static Timestamp toTimestamp(String text) {
        try {
            return text == null ? null : Timestamp.valueOf(text);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** The current date and time. */
    private static Timestamp now() {
        return new Timestamp(System.currentTimeMillis());
    }
}

