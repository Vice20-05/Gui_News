import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import DataHandle.Event;
import DataHandle.News;

/** Displays news in a searchable list (left, 1/3) with a details panel (right, 2/3). */
public class NewsPanel extends JPanel {
    // Cover images are scaled down to this height in the details panel.
    private static final int COVER_HEIGHT = 200;

    // allNews holds every news item; visibleNews only the ones that match the search.
    private final List<News> allNews = new ArrayList<>();
    private final DefaultListModel<News> visibleNews = new DefaultListModel<>();
    private final JList<News> newsList = new JList<>(visibleNews);

    // The parts of the window.
    private final JTextField searchField = new JTextField();
    // "Both" shows every language; the others keep only RO or HU news.
    private final JComboBox<String> languageBox = new JComboBox<>(new String[] {"Both", "Romanian", "Hungarian"});
    private final JButton refreshButton = new JButton("Refresh");
    private final JButton addButton = new JButton("Add");
    private final JButton editButton = new JButton("Edit");
    private final JButton deleteButton = new JButton("Delete");
    private final JButton clearDbButton = new JButton("Clear DB");
    // Switch the list between news and events.
    private final JButton newsButton = new JButton("News");
    private final JButton eventsButton = new JButton("Events");
    private final JLabel statusLabel = new JLabel(" ");
    private final JProgressBar loadingBar = new JProgressBar();
    private final JPanel detailsPanel = new JPanel(new BorderLayout(12, 12));

    /** Creates the search bar, list, details area, buttons and status bar. */
    public NewsPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // Show the details whenever the user clicks a different item.
        newsList.addListSelectionListener(event -> {
            // Skip the extra events Swing sends while the mouse button is still held down.
            if (!event.getValueIsAdjusting()) {
                showDetails(newsList.getSelectedValue());
            }
        });

        // Filter the list while the user is typing.
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                filterNews();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                filterNews();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                filterNews();
            }
        });

        // Filter the list again when another language is chosen.
        languageBox.addActionListener(event -> filterNews());

        showDetails(null);

        // Top bar: "Search: [__________] Language: [Both v] [Refresh]"
        JPanel topBar = new JPanel(new BorderLayout(8, 8));
        topBar.add(new JLabel("Search:"), BorderLayout.WEST);
        topBar.add(searchField, BorderLayout.CENTER);
        JPanel topRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        topRight.add(new JLabel("Language:"));
        topRight.add(languageBox);
        topRight.add(refreshButton);
        topBar.add(topRight, BorderLayout.EAST);

        JSplitPane splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(newsList), detailsPanel);
        // The list gets about 1/3 of the window, the details about 2/3.
        splitPane.setResizeWeight(1.0 / 3.0);
        splitPane.setDividerLocation(1.0 / 3.0);

        // Button bar under the list and details: [Add] [Edit] [Delete] [News] [Events] [Clear DB]
        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttonBar.add(addButton);
        buttonBar.add(editButton);
        buttonBar.add(deleteButton);
        buttonBar.add(newsButton);
        buttonBar.add(eventsButton);
        buttonBar.add(clearDbButton);

        // Status bar: status text and a progress bar that only shows while loading.
        loadingBar.setIndeterminate(true);
        loadingBar.setVisible(false);
        JPanel statusBar = new JPanel(new BorderLayout(8, 8));
        statusBar.add(statusLabel, BorderLayout.CENTER);
        statusBar.add(loadingBar, BorderLayout.EAST);

        JPanel bottomPanel = new JPanel(new BorderLayout(8, 8));
        bottomPanel.add(buttonBar, BorderLayout.NORTH);
        bottomPanel.add(statusBar, BorderLayout.SOUTH);

        add(topBar, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    // These methods let GUI decide what each button does.
    public void onRefresh(Runnable action) { refreshButton.addActionListener(event -> action.run()); }
    public void onAdd(Runnable action) { addButton.addActionListener(event -> action.run()); }
    public void onEdit(Runnable action) { editButton.addActionListener(event -> action.run()); }
    public void onDelete(Runnable action) { deleteButton.addActionListener(event -> action.run()); }
    public void onClearDb(Runnable action) { clearDbButton.addActionListener(event -> action.run()); }
    public void onShowNews(Runnable action) { newsButton.addActionListener(event -> action.run()); }
    public void onShowEvents(Runnable action) { eventsButton.addActionListener(event -> action.run()); }

    /** Returns the news item selected in the list, or null if nothing is selected. */
    public News getSelectedNews() {
        return newsList.getSelectedValue();
    }

    /** Shows or hides the loading state and blocks the buttons while loading. */
    public void setLoading(boolean loading) {
        loadingBar.setVisible(loading);
        // Changing the database while a refresh is writing to it would mix up the data.
        refreshButton.setEnabled(!loading);
        addButton.setEnabled(!loading);
        editButton.setEnabled(!loading);
        deleteButton.setEnabled(!loading);
        clearDbButton.setEnabled(!loading);
        newsButton.setEnabled(!loading);
        eventsButton.setEnabled(!loading);
        revalidate();
    }

    /** Shows a message in the status bar. */
    public void setStatus(String text) {
        statusLabel.setText(text);
    }

    /** Replaces all items in the list with new ones (news or events, since Event extends News). */
    public void setNews(List<? extends News> news) {
        allNews.clear();
        allNews.addAll(news);
        filterNews();
    }

    /** Rebuilds the visible list from allNews, keeping only the items that match the search and language. */
    private void filterNews() {
        String searchText = searchField.getText().trim().toLowerCase(Locale.ROOT);
        String language = selectedLanguageCode();
        visibleNews.clear();

        for (News news : allNews) {
            if (matchesSearch(news, searchText) && matchesLanguage(news, language)) {
                visibleNews.addElement(news);
            }
        }

        // Select the first result so the details panel is never empty when there are results.
        if (!visibleNews.isEmpty()) {
            newsList.setSelectedIndex(0);
        } else {
            showDetails(null);
        }
    }

    /** Returns the language code chosen in the dropdown, or "" when both languages are shown. */
    private String selectedLanguageCode() {
        String selected = (String) languageBox.getSelectedItem();
        if ("Romanian".equals(selected)) {
            return "RO";
        }
        if ("Hungarian".equals(selected)) {
            return "HU";
        }
        return "";
    }

    /** Checks whether the news is in the chosen language; an empty code matches everything. */
    private boolean matchesLanguage(News news, String languageCode) {
        return languageCode.isEmpty() || news.getLanguage().equalsIgnoreCase(languageCode);
    }

    /** Checks whether the search text appears in the title or description. */
    private boolean matchesSearch(News news, String searchText) {
        if (searchText.isEmpty()) {
            return true;
        }
        String title = news.getTitle().toLowerCase(Locale.ROOT);
        String description = news.getDescription().toLowerCase(Locale.ROOT);
        return title.contains(searchText) || description.contains(searchText);
    }

    /** Shows all fields of the news item on the right side, or a hint when nothing is selected. */
    private void showDetails(News news) {
        // Remove the old details before adding the new ones.
        detailsPanel.removeAll();

        JTextArea details = new JTextArea();
        details.setEditable(false);
        details.setLineWrap(true);
        details.setWrapStyleWord(true);

        if (news == null) {
            details.setText("Select an item to see its details.");
        } else {
            // Events show when and where they happen right under the title.
            String eventInfo = "";
            if (news instanceof Event event) {
                eventInfo = "Starts: " + formatDate(event.getEventDateTime()) + "\n"
                        + "Location: " + event.getLocation() + "\n\n";
            }
            details.setText(news.getTitle() + "\n\n"
                    + eventInfo
                    + news.getDescription() + "\n\n"
                    + "Source: " + news.getSource() + "\n"
                    + "Author: " + news.getAuthor() + "\n"
                    + "Publisher: " + news.getPublisher() + "\n"
                    + "Published: " + formatDate(news.getPublishDate()) + "\n"
                    + "Language: " + news.getLanguage() + "\n"
                    + "URL: " + news.getUrl() + "\n\n"
                    + "Saved: " + formatDate(news.getCreationDate()) + "\n"
                    + "Last modified: " + formatDate(news.getLastModifiedDate()));
            // Scroll back to the top for long descriptions.
            details.setCaretPosition(0);

            // The cover image is shown as a picture above the text.
            detailsPanel.add(createCoverLabel(news.getCoverImage()), BorderLayout.NORTH);
        }

        detailsPanel.add(new JScrollPane(details), BorderLayout.CENTER);
        // Tell Swing the panel changed, so it lays it out and draws it again.
        detailsPanel.revalidate();
        detailsPanel.repaint();
    }

    /** Formats a Timestamp as "yyyy-MM-dd HH:mm:ss", or "" when it is missing. */
    public static String formatDate(Timestamp timestamp) {
        // Timestamp.toString() gives "2026-10-20 18:30:00.0"; we cut off the ".0".
        return timestamp == null ? "" : timestamp.toString().substring(0, 19);
    }

    /** Creates a label with the cover image, or the default image if it can't be loaded. */
    private JLabel createCoverLabel(String imagePath) {
        BufferedImage image = readImage(imagePath);
        if (image == null) {
            // Missing or broken file: fall back to the default cover.
            image = readImage(News.DEFAULT_COVER_IMAGE);
        }
        if (image == null) {
            return new JLabel("No cover image", SwingConstants.CENTER);
        }

        // Keep the aspect ratio while scaling the image to a fixed height.
        int width = image.getWidth() * COVER_HEIGHT / image.getHeight();
        Image scaled = image.getScaledInstance(width, COVER_HEIGHT, Image.SCALE_SMOOTH);
        return new JLabel(new ImageIcon(scaled));
    }

    /** Reads an image file, returning null if the path is empty, missing, or not an image. */
    private BufferedImage readImage(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        File file = new File(path);
        if (!file.isFile()) {
            return null;
        }
        try {
            return ImageIO.read(file);
        } catch (IOException e) {
            return null;
        }
    }
}
