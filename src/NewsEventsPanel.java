import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
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
    private final JPanel detailsPanel = new JPanel(new BorderLayout(12, 12));

    /** Creates the list, search field, and details area. */
    public NewsEventsPanel() {
        setLayout(new BorderLayout(8, 8));

        itemList.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                showDetails(itemList.getSelectedValue());
            }
        });

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

        JPanel leftPanel = new JPanel(new BorderLayout(8, 8));
        leftPanel.add(searchField, BorderLayout.NORTH);
        leftPanel.add(new JScrollPane(itemList), BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, detailsPanel);
        splitPane.setResizeWeight(0.35);
        add(splitPane, BorderLayout.CENTER);
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

    private boolean matchesSearch(ContentItems item, String searchText) {
        if (item.getType().toLowerCase(Locale.ROOT).contains(searchText)) {
            return true;
        }

        for (String value : item.getValues()) {
            if (value.toLowerCase(Locale.ROOT).contains(searchText)) {
                return true;
            }
        }

        return false;
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
        }

        detailsPanel.add(new JScrollPane(details), BorderLayout.CENTER);
        detailsPanel.revalidate();
        detailsPanel.repaint();
    }
}
