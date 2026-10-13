
import java.awt.Dimension;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import DataHandle.Event;
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
    // true while the list shows events, false while it shows news.
    // Add, Edit and Delete look at this to know which table to change.
    private static boolean showingEvents = false;

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
        panel.onShowNews(() -> switchView(false));
        panel.onShowEvents(() -> switchView(true));

        frame = new JFrame("News Manager");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(new Dimension(1000, 650));
        frame.setLocationRelativeTo(null);
        frame.add(panel);
        frame.setVisible(true);

        loadSavedNews();
    }

    /** Shows saved news first, then refreshes it from the configured RSS feeds. */
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
                    // Refresh on every start so existing installations receive new articles too.
                    refresh();
                } catch (InterruptedException | ExecutionException e) {
                    if (e instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                    }
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
        new SwingWorker<List<? extends News>, Void>() {
            // Messages about feeds that failed, filled in by the background work.
            private List<String> problems;

            // Runs on a background thread, so downloading doesn't freeze the window.
            @Override
            protected List<? extends News> doInBackground() throws Exception {
                // Download all feeds, save the new news, then read the current list back.
                problems = RssUpdater.updateAll(RSS_CONFIG.toString(), database);
                return loadItems();
            }

            // Runs back on the Swing thread to show the result.
            @Override
            protected void done() {
                panel.setLoading(false);
                try {
                    List<? extends News> news = get();
                    panel.setNews(news);
                    if (problems.isEmpty()) {
                        panel.setStatus("Refreshed: " + news.size() + " " + itemName() + ".");
                    } else {
                        // Show which feeds failed and why (for example "Punctul: 403 Forbidden").
                        panel.setStatus("Refreshed: " + news.size() + " " + itemName() + ". Failed: "
                                + String.join(", ", problems));
                    }
                } catch (InterruptedException | ExecutionException e) {
                    if (e instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                    }
                    panel.setStatus("Refresh failed: " + rootMessage(e));
                }
            }
        }.execute();
    }

    /** News / Events buttons: shows the other list. */
    private static void switchView(boolean events) {
        showingEvents = events;
        try {
            reloadList(events ? "Showing events." : "Showing news.");
        } catch (SQLException e) {
            showError("Could not load the " + itemName() + ": " + e.getMessage());
        }
    }

    /** Add button: asks for the data of a new news item (or event) and saves it. */
    private static void addNews() {
        News news = NewsForm.show(frame, null, showingEvents);
        if (news == null) {
            return; // the user pressed Cancel
        }
        try {
            if (news instanceof Event event) {
                if (!database.addEvent(event)) {
                    showError("An event with this title, start time and location already exists.");
                    return;
                }
                database.save();
                reloadList("Event added.");
                return;
            }
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
            showError("Select an item first.");
            return;
        }
        News changed = NewsForm.show(frame, selected, showingEvents);
        if (changed == null) {
            return; // the user pressed Cancel
        }
        try {
            if (selected instanceof Event original) {
                // The old event is passed too, because its key may have been edited.
                database.updateEvent(original, (Event) changed);
                database.save();
                reloadList("Event updated.");
                return;
            }
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
            showError("Select an item first.");
            return;
        }
        int result = JOptionPane.showConfirmDialog(frame,
                "Delete \"" + selected.getTitle() + "\"?", "Delete", JOptionPane.YES_NO_OPTION);
        if (result != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            if (selected instanceof Event event) {
                database.deleteEvent(event.getTitle(), event.getEventDateTime(), event.getLocation());
                database.save();
                reloadList("Event deleted.");
                return;
            }
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
                "Are you sure? This deletes all saved news and events.",
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

    /** Reads the current list (news or events) from the database again and shows it with a status message. */
    private static void reloadList(String status) throws SQLException {
        panel.setNews(loadItems());
        panel.setStatus(status);
    }

    /** Returns all events or all news from the database, depending on which list is shown. */
    private static List<? extends News> loadItems() throws SQLException {
        return showingEvents ? database.getAllEvents() : database.getAllNews();
    }

    /** "events" or "news", for status messages. */
    private static String itemName() {
        return showingEvents ? "events" : "news";
    }

    /** Shows an error message in a pop-up. */
    private static void showError(String message) {
        JOptionPane.showMessageDialog(frame, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    /** Returns the message of the original exception thrown in the background thread. */
    private static String rootMessage(Exception e) {
        Throwable cause = e;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        String message = cause.getMessage();
        return message == null || message.isBlank() ? cause.getClass().getSimpleName() : message;
    }
}
