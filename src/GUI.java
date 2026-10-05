
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

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
        DefaultListModel<ContentItems> items = new DefaultListModel<>();
        items.addElement(new ContentItems("News", new String[]{
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
        items.addElement(new ContentItems("Event", new String[]{
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

        JList<ContentItems> itemList = new JList<>(items);
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel("News and Events"), BorderLayout.NORTH);
        panel.add(new JScrollPane(itemList), BorderLayout.CENTER);
        return panel;
    }
}
