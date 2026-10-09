
import DataHandle.RssArticle;
import Database.NewsDatabase;
import Database.RssUpdater;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

public class GUI {
    private static final String RSS_JSON_FILE = "RSS.json";
    private static final String DATABASE_FILE = "news.db";

    // Show the GUI
    public static void showGUI() {
        JFrame frame = new JFrame("News and Events");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(new Dimension(800, 600));
        frame.setLocationRelativeTo(null);
        frame.add(createContentPanel());
        frame.setVisible(true);
    }

    //Display the content panel with news loaded from the SQLite database
    private static JPanel createContentPanel() {
        NewsEventsPanel panel = new NewsEventsPanel();
        JLabel statusLabel = new JLabel("Loading news...");

        // Downloading feeds is slow, so we do it in a SwingWorker (a background thread).
        // Otherwise the window would freeze until every website has answered.
        new SwingWorker<List<NewsDatabase.StoredArticle>, Void>() {
            @Override
            protected List<NewsDatabase.StoredArticle> doInBackground() throws Exception {
                NewsDatabase database = new NewsDatabase(DATABASE_FILE);
                new RssUpdater(database).updateAll(RSS_JSON_FILE);
                return database.loadArticles();
            }

            // done() runs back on the GUI thread, where it is safe to change the window.
            @Override
            protected void done() {
                try {
                    List<NewsDatabase.StoredArticle> articles = get();
                    for (NewsDatabase.StoredArticle stored : articles) {
                        panel.addItem(toContentItem(stored));
                    }
                    statusLabel.setText("News and Events (" + articles.size() + " articles)");
                } catch (InterruptedException | ExecutionException e) {
                    statusLabel.setText("Could not load news: " + e.getMessage());
                }
            }
        }.execute();

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(statusLabel, BorderLayout.NORTH);
        contentPanel.add(panel, BorderLayout.CENTER);
        return contentPanel;
    }

    // Turn one database row into the item format the NewsEventsPanel shows.
    private static ContentItems toContentItem(NewsDatabase.StoredArticle stored) {
        RssArticle article = stored.article();
        return new ContentItems("News", new String[]{
                "Title", "Description", "Publish date", "Source", "Source link", "Language"
        }, new String[]{
                article.getTitle(),
                article.getDescription(),
                article.getPublishDate(),
                stored.sourceName(),
                article.getUrl(),
                article.getLanguage()
        });
    }
}
