
import java.awt.Dimension;
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
    private static final Path RSS_CONFIG = Path.of("RSS.json");

    private static NewsDatabase database;
    private static NewsEventsPanel panel;

    /** Creates and displays the application window. */
    public static void showGUI() {
        try {
            database = new NewsDatabase();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Could not open the database: " + e.getMessage(),
                    "Database error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        panel = new NewsEventsPanel();
        panel.onRefresh(GUI::refresh);

        JFrame frame = new JFrame("News and Events");
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
            @Override
            protected List<RssArticle> doInBackground() throws Exception {
                return database.loadArticles();
            }

            @Override
            protected void done() {
                panel.setLoading(false);
                try {
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
            @Override
            protected List<RssArticle> doInBackground() throws Exception {
                RssUpdater.updateAll(RSS_CONFIG.toString(), database);
                return database.loadArticles();
            }

            @Override
            protected void done() {
                panel.setLoading(false);
                try {
                    List<RssArticle> articles = get();
                    showArticles(articles);
                    panel.setStatus("Refreshed: " + articles.size() + " articles.");
                } catch (InterruptedException | ExecutionException e) {
                    panel.setStatus("Refresh failed: " + rootMessage(e));
                }
            }
        }.execute();
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
                "Title", "Description", "Publish date", "Source", "Source link", "Language"
        };
        String[] values = {
                article.getTitle(),
                article.getDescription(),
                article.getPublishDate(),
                article.getSourceName(),
                article.getUrl(),
                article.getLanguage()
        };
        return new ContentItems("News", fieldNames, values);
    }

    /** Returns the message of the original exception thrown in the background thread. */
    private static String rootMessage(Exception e) {
        Throwable cause = e.getCause() != null ? e.getCause() : e;
        return cause.getMessage();
    }
}
