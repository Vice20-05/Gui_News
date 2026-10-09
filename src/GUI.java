
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class GUI {
    // Show the GUI
    public static void showGUI() {
        JFrame frame = new JFrame("News and Events");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(new Dimension(800, 600));
        frame.setLocationRelativeTo(null);
        frame.add(createContentPanel());
        frame.setVisible(true);
    }

    //Display the content panel with sample news and events
    private static JPanel createContentPanel() {
        NewsEventsPanel panel = new NewsEventsPanel();
        panel.addItem(new ContentItems("News", new String[]{
                "Title", "Description", "Publish date", "Author",
                "Source", "Source link", "Cover image", "Language"
        }, new String[]{
                "Sample news story",
                "A short example description of a news story.",
                "2026-10-05",
                "Alex",
                "Example News",
                "https://example.com/news",
                "",
                "English"
        }));
        panel.addItem(new ContentItems("Event", new String[]{
                "Title", "Description", "Event date", "Event time",
                "Location", "Source", "Source link", "Cover image", "Language"
        }, new String[]{
                "Sample event",
                "An example event for the list.",
                "2026-10-12",
                "14:00",
                "City Hall",
                "Example Events",
                "https://example.com/events",
                "",
                "English"
        }));

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(new JLabel("News and Events"), BorderLayout.NORTH);
        contentPanel.add(panel, BorderLayout.CENTER);
        return contentPanel;
    }
}
