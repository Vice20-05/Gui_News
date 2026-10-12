import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** Displays items in a searchable list with a details panel. */
public class NewsEventsPanel extends JPanel {
    private final List<ContentItems> allItems = new ArrayList<>();
    private final DefaultListModel<ContentItems> visibleItems = new DefaultListModel<>();
    private final JList<ContentItems> itemList = new JList<>(visibleItems);
    private final JTextField searchField = new JTextField();
    private final JButton refreshButton = new JButton("Refresh");
    private final JLabel statusLabel = new JLabel(" ");
    private final JProgressBar loadingBar = new JProgressBar();
    private final JPanel detailsPanel = new JPanel(new BorderLayout(12, 12));

    /** Creates the search bar, list, details area, and status bar. */
    public NewsEventsPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        itemList.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                showDetails(itemList.getSelectedValue());
            }
        });

        // Filter the list while the user is typing.
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                filterItems();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                filterItems();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                filterItems();
            }
        });

        showDetails(null);

        // Top bar: "Search: [__________] [Refresh]"
        JPanel topBar = new JPanel(new BorderLayout(8, 8));
        topBar.add(new JLabel("Search:"), BorderLayout.WEST);
        topBar.add(searchField, BorderLayout.CENTER);
        topBar.add(refreshButton, BorderLayout.EAST);

        JSplitPane splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(itemList), detailsPanel);
        // The list gets about 1/3 of the window, the details about 2/3.
        splitPane.setResizeWeight(1.0 / 3.0);
        splitPane.setDividerLocation(1.0 / 3.0);

        // Bottom bar: status text and a progress bar that only shows while loading.
        loadingBar.setIndeterminate(true);
        loadingBar.setVisible(false);
        JPanel statusBar = new JPanel(new BorderLayout(8, 8));
        statusBar.add(statusLabel, BorderLayout.CENTER);
        statusBar.add(loadingBar, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);
    }

    /** Runs the given action when the Refresh button is clicked. */
    public void onRefresh(Runnable action) {
        refreshButton.addActionListener(event -> action.run());
    }

    /** Shows or hides the loading state and blocks the Refresh button while loading. */
    public void setLoading(boolean loading) {
        loadingBar.setVisible(loading);
        refreshButton.setEnabled(!loading);
        revalidate();
    }

    /** Shows a message in the status bar. */
    public void setStatus(String text) {
        statusLabel.setText(text);
    }

    /** Adds an item and refreshes the filtered list. */
    public void addItem(ContentItems item) {
        if (item == null) {
            throw new IllegalArgumentException("item cannot be null");
        }
        allItems.add(item);
        filterItems();
    }

    /** Replaces all items in the list with new ones. */
    public void setItems(List<ContentItems> items) {
        // Remove the old items first, otherwise every article would show up twice.
        allItems.clear();
        allItems.addAll(items);

        // Refresh the list only ONCE at the end.
        // (Calling addItem() for each article refreshed it every time, which was slow.)
        filterItems();
    }

    private void filterItems() {
        String searchText = searchField.getText().trim().toLowerCase(Locale.ROOT);
        visibleItems.clear();

        for (ContentItems item : allItems) {
            if (matchesSearch(item, searchText)) {
                visibleItems.addElement(item);
            }
        }

        if (!visibleItems.isEmpty()) {
            itemList.setSelectedIndex(0);
        } else {
            showDetails(null);
        }
    }

    /** Checks whether the search text appears in the title or description. */
    private boolean matchesSearch(ContentItems item, String searchText) {
        if (searchText.isEmpty()) {
            return true;
        }
        String title = item.getValue("Title").toLowerCase(Locale.ROOT);
        String description = item.getValue("Description").toLowerCase(Locale.ROOT);
        return title.contains(searchText) || description.contains(searchText);
    }

    private void showDetails(ContentItems item) {
        detailsPanel.removeAll();

        JTextArea details = new JTextArea();
        details.setEditable(false);
        details.setLineWrap(true);
        details.setWrapStyleWord(true);

        if (item == null) {
            details.setText("Select an item to see its details.");
        } else {
            StringBuilder text = new StringBuilder(item.getType()).append("\n\n");
            String[] fieldNames = item.getFieldNames();
            String[] values = item.getValues();

            for (int i = 0; i < fieldNames.length && i < values.length; i++) {
                text.append(fieldNames[i]).append(": ").append(values[i]).append("\n\n");
            }

            details.setText(text.toString());
            // Scroll back to the top for long descriptions.
            details.setCaretPosition(0);
        }

        detailsPanel.add(new JScrollPane(details), BorderLayout.CENTER);
        detailsPanel.revalidate();
        detailsPanel.repaint();
    }
}
