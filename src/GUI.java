
import java.awt.Dimension;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import DataHandle.News;
import DataHandle.NewsDatabase;
import DataHandle.RssUpdater;

/** Builds the main window and connects every button to the database. */
public class GUI {
    // The JSON file with the list of RSS feeds to download.
    private static final Path RSS_CONFIG = Path.of("res", "rss.json");

    // Created once in showGUI() and shared by all the methods below.
    private static NewsDatabase database;
    private static NewsPanel panel;
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
        panel = new NewsPanel();
        panel.onRefresh(GUI::refresh);
        panel.onAdd(GUI::addNews);
        panel.onEdit(GUI::editNews);
        panel.onDelete(GUI::deleteNews);
        panel.onClearDb(GUI::clearDatabase);

        frame = new JFrame("News Manager");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(new Dimension(1000, 650));
        frame.setLocationRelativeTo(null);
        frame.add(panel);
        frame.setVisible(true);

        loadSavedNews();
    }

    /** Shows the news already saved in SQLite, without going online. */
    private static void loadSavedNews() {
        panel.setLoading(true);
        panel.setStatus("Loading saved news...");

        new SwingWorker<List<News>, Void>() {
            // Runs on a background thread, so reading the database doesn't freeze the window.
            @Override
            protected List<News> doInBackground() throws Exception {
                return database.getAllNews();
            }

            // Runs back on the Swing thread when the work is finished, so it can update the UI.
            @Override
            protected void done() {
                panel.setLoading(false);
                try {
                    // get() returns what doInBackground() returned, or throws its error.
                    List<News> news = get();
                    panel.setNews(news);
                    if (news.isEmpty()) {
                        // First start: nothing saved yet, so download the feeds right away.
                        refresh();
                    } else {
                        panel.setStatus(news.size() + " saved news. Press Refresh to download new ones.");
                    }
                } catch (InterruptedException | ExecutionException e) {
                    panel.setStatus("Could not load saved news: " + rootMessage(e));
                }
            }
        }.execute();
    }

    /** Downloads every RSS feed, saves new news, and reloads the list. */
    private static void refresh() {
        panel.setLoading(true);
        panel.setStatus("Downloading news from RSS feeds...");

        // Download RSS feeds in the background to keep the Swing UI responsive.
        new SwingWorker<List<News>, Void>() {
            // Messages about feeds that failed, filled in by the background work.
            private List<String> problems;

            // Runs on a background thread, so downloading doesn't freeze the window.
            @Override
            protected List<News> doInBackground() throws Exception {
                // Download all feeds, save the new news, then read the full list back.
                problems = RssUpdater.updateAll(RSS_CONFIG.toString(), database);
                return database.getAllNews();
            }

            // Runs back on the Swing thread to show the result.
            @Override
            protected void done() {
                panel.setLoading(false);
                try {
                    List<News> news = get();
                    panel.setNews(news);
                    if (problems.isEmpty()) {
                        panel.setStatus("Refreshed: " + news.size() + " news.");
                    } else {
                        // Show which feeds failed and why (for example "Punctul: 403 Forbidden").
                        panel.setStatus("Refreshed: " + news.size() + " news. Failed: "
                                + String.join(", ", problems));
                    }
                } catch (InterruptedException | ExecutionException e) {
                    panel.setStatus("Refresh failed: " + rootMessage(e));
                }
            }
        }.execute();
    }

    /** Add button: asks for the data of a new news item and saves it. */
    private static void addNews() {
        News news = NewsForm.show(frame, null);
        if (news == null) {
            return; // the user pressed Cancel
        }
        try {
            if (!database.addNews(news)) {
                showError("A news item with this source and URL already exists.");
                return;
            }
            database.save();
            reloadList("News added.");
        } catch (SQLException e) {
            showError("Could not add the news: " + e.getMessage());
        }
    }

    /** Edit button: lets the user change the selected news item and saves the changes. */
    private static void editNews() {
        News selected = panel.getSelectedNews();
        if (selected == null) {
            showError("Select a news item first.");
            return;
        }
        News changed = NewsForm.show(frame, selected);
        if (changed == null) {
            return; // the user pressed Cancel
        }
        try {
            database.updateNews(changed);
            database.save();
            reloadList("News updated.");
        } catch (SQLException e) {
            showError("Could not update the news: " + e.getMessage());
        }
    }

    /** Delete button: removes the selected news item after the user confirms. */
    private static void deleteNews() {
        News selected = panel.getSelectedNews();
        if (selected == null) {
            showError("Select a news item first.");
            return;
        }
        int result = JOptionPane.showConfirmDialog(frame,
                "Delete \"" + selected.getTitle() + "\"?", "Delete news", JOptionPane.YES_NO_OPTION);
        if (result != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            database.deleteNews(selected.getSource(), selected.getUrl());
            database.save();
            reloadList("News deleted.");
        } catch (SQLException e) {
            showError("Could not delete the news: " + e.getMessage());
        }
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
            panel.setNews(List.of());
            panel.setStatus("Database cleared. Press Refresh to download the news again.");
        } catch (SQLException e) {
            showError("Could not clear the database: " + e.getMessage());
        }
    }

    /** Reads all news from the database again and shows them with a status message. */
    private static void reloadList(String status) throws SQLException {
        panel.setNews(database.getAllNews());
        panel.setStatus(status);
    }

    /** Shows an error message in a pop-up. */
    private static void showError(String message) {
        JOptionPane.showMessageDialog(frame, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    /** Returns the message of the original exception thrown in the background thread. */
    private static String rootMessage(Exception e) {
        Throwable cause = e.getCause() != null ? e.getCause() : e;
        return cause.getMessage();
    }
}
