
import java.awt.Dimension;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import DataHandle.NewsDatabase;
import DataHandle.RssArticle;
import DataHandle.RssUpdater;

/** Builds the main window, shows saved news, and refreshes RSS feeds on demand. */
public class GUI {
    // The JSON file with the list of RSS feeds to download.
    private static final Path RSS_CONFIG = Path.of("RSS.json");

    // Created once in showGUI() and shared by all the methods below.
    private static NewsDatabase database;
    private static NewsEventsPanel panel;
    private static JFrame frame;

    /** Creates and displays the application window. */
    public static void showGUI() {
        // Without a database the app can't do anything, so show an error and stop.
        try {
            database = new NewsDatabase();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Could not open the database: " + e.getMessage(),
                    "Database error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Tell the panel which method each button should call.
        panel = new NewsEventsPanel();
        panel.onRefresh(GUI::refresh);
        panel.onClearDb(GUI::clearDatabase);

        frame = new JFrame("News and Events");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(new Dimension(1000, 650));
        frame.setLocationRelativeTo(null);
        frame.add(panel);
        frame.setVisible(true);

        loadSavedArticles();
    }

    /** Shows the articles already saved in SQLite, without going online. */
    private static void loadSavedArticles() {
        panel.setLoading(true);
        panel.setStatus("Loading saved news...");

        new SwingWorker<List<RssArticle>, Void>() {
            // Runs on a background thread, so reading the database doesn't freeze the window.
            @Override
            protected List<RssArticle> doInBackground() throws Exception {
                try {
                    // Older saved articles have no language, so take it from RSS.json.
                    RssUpdater.fillMissingLanguages(RSS_CONFIG.toString(), database);
                } catch (IOException e) {
                    // Without RSS.json we can still show the saved news.
                    System.out.println("Could not read languages from RSS.json: " + e.getMessage());
                }
                return database.loadArticles();
            }

            // Runs back on the Swing thread when the work is finished, so it can update the UI.
            @Override
            protected void done() {
                panel.setLoading(false);
                try {
                    // get() returns what doInBackground() returned, or throws its error.
                    List<RssArticle> articles = get();
                    showArticles(articles);
                    if (articles.isEmpty()) {
                        // First start: nothing saved yet, so download the feeds right away.
                        refresh();
                    } else {
                        panel.setStatus(articles.size() + " saved articles. Press Refresh to download new ones.");
                    }
                } catch (InterruptedException | ExecutionException e) {
                    panel.setStatus("Could not load saved news: " + rootMessage(e));
                }
            }
        }.execute();
    }

    /** Downloads every RSS feed, saves new articles, and reloads the list. */
    private static void refresh() {
        panel.setLoading(true);
        panel.setStatus("Downloading news from RSS feeds...");

        // Download RSS feeds in the background to keep the Swing UI responsive.
        new SwingWorker<List<RssArticle>, Void>() {
            // Messages about feeds that failed, filled in by the background work.
            private List<String> problems;

            // Runs on a background thread, so downloading doesn't freeze the window.
            @Override
            protected List<RssArticle> doInBackground() throws Exception {
                // Download all feeds, save the new articles, then read the full list back.
                problems = RssUpdater.updateAll(RSS_CONFIG.toString(), database);
                return database.loadArticles();
            }

            // Runs back on the Swing thread to show the result.
            @Override
            protected void done() {
                panel.setLoading(false);
                try {
                    List<RssArticle> articles = get();
                    showArticles(articles);
                    if (problems.isEmpty()) {
                        panel.setStatus("Refreshed: " + articles.size() + " articles.");
                    } else {
                        // Show which feeds failed and why (for example "Punctul: 403 Forbidden").
                        panel.setStatus("Refreshed: " + articles.size() + " articles. Failed: "
                                + String.join(", ", problems));
                    }
                } catch (InterruptedException | ExecutionException e) {
                    panel.setStatus("Refresh failed: " + rootMessage(e));
                }
            }
        }.execute();
    }

    /** Deletes all news from the database after the user confirms. */
    private static void clearDatabase() {
        int result = JOptionPane.showConfirmDialog(
                frame,
                "Are you sure? This deletes all saved news.",
                "Clear database",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (result != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            database.clearAll();
            showArticles(List.of());
            panel.setStatus("Database cleared. Press Refresh to download the news again.");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame, "Could not clear the database: " + e.getMessage(),
                    "Database error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Replaces the list in the panel with the given articles. */
    private static void showArticles(List<RssArticle> articles) {
        // Turn every article into an item the panel understands.
        List<ContentItems> items = new ArrayList<>();
        for (RssArticle article : articles) {
            items.add(toContentItem(article));
        }
        panel.setItems(items);
    }

    /** Converts a stored RSS article into the generic item used by the UI. */
    private static ContentItems toContentItem(RssArticle article) {
        String[] fieldNames = {
                "Title", "Description", "Publish date", "Source", "Source link", "Language", "Cover image"
        };
        String[] values = {
                article.getTitle(),
                article.getDescription(),
                article.getPublishDate(),
                article.getSourceName(),
                article.getUrl(),
                article.getLanguage(),
                article.getCoverImage()
        };
        return new ContentItems("News", fieldNames, values);
    }

    /** Returns the message of the original exception thrown in the background thread. */
    private static String rootMessage(Exception e) {
        Throwable cause = e.getCause() != null ? e.getCause() : e;
        return cause.getMessage();
    }
}
