
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import DataHandle.NewsDatabase;
import DataHandle.RssArticle;
import DataHandle.RssUpdater;

/** Builds the main window and loads RSS articles in the background. */
public class GUI {
    private static final Path RSS_CONFIG = Path.of("RSS.json");

    /** Creates and displays the application window. */
    public static void showGUI() {
        JFrame frame = new JFrame("News and Events");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(new Dimension(800, 600));
        frame.setLocationRelativeTo(null);
        frame.add(createContentPanel());
        frame.setVisible(true);
    }

    /** Creates the panel that displays downloaded articles and loading status. */
    private static JPanel createContentPanel() {
        NewsEventsPanel panel = new NewsEventsPanel();
        JLabel statusLabel = new JLabel("Loading news and events...");
        // Downloading is slow, so we do it on a separate thread.
        // That way the window does not freeze while we wait.
        Thread worker = new Thread(() -> {
            try {
                NewsDatabase database = new NewsDatabase();

                // Step 1: show the articles we already saved last time.
                // This fills the list right away instead of leaving it empty.
                List<RssArticle> savedArticles = database.loadArticles();
                SwingUtilities.invokeLater(() -> {
                    showArticles(panel, savedArticles);
                    statusLabel.setText("Showing " + savedArticles.size()
                            + " saved articles, downloading new ones...");
                });

                // Step 2: download new articles from every website (this takes a while).
                RssUpdater.updateAll(RSS_CONFIG.toString(), database);

                // Step 3: load everything again, so the new articles appear too.
                List<RssArticle> articles = database.loadArticles();
                SwingUtilities.invokeLater(() -> {
                    showArticles(panel, articles);
                    statusLabel.setText("News and Events (" + articles.size() + " articles)");
                });
            } catch (IOException | SQLException e) {
                SwingUtilities.invokeLater(
                        () -> statusLabel.setText("Could not load news: " + e.getMessage())
                );
            }
        });
        worker.start();

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(statusLabel, BorderLayout.NORTH);
        contentPanel.add(panel, BorderLayout.CENTER);
        return contentPanel;
    }

    /** Replaces the list in the panel with the given articles. */
    private static void showArticles(NewsEventsPanel panel, List<RssArticle> articles) {
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
}
