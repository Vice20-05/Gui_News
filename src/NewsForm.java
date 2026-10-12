import java.awt.Component;
import java.awt.GridLayout;
import java.sql.Timestamp;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import DataHandle.News;

/** A pop-up form for adding a new news item or editing an existing one. */
public class NewsForm {
    /**
     * Shows the form. Pass null to add a new item, or an existing item to edit it.
     * Returns the filled-in News, or null if the user pressed Cancel.
     */
    public static News show(Component parent, News existing) {
        boolean editing = existing != null;

        // One text field for every value the user can type.
        JTextField titleField = new JTextField(30);
        JTextField descriptionField = new JTextField(30);
        JTextField sourceField = new JTextField("Manual", 30);
        JTextField urlField = new JTextField(30);
        JTextField authorField = new JTextField(30);
        JTextField publisherField = new JTextField(30);
        JTextField dateField = new JTextField(NewsPanel.formatDate(new Timestamp(System.currentTimeMillis())), 30);
        JComboBox<String> languageBox = new JComboBox<>(new String[] {"RO", "HU"});
        JTextField coverField = new JTextField(30);

        // When editing, fill the form with the current values.
        if (editing) {
            titleField.setText(existing.getTitle());
            descriptionField.setText(existing.getDescription());
            sourceField.setText(existing.getSource());
            urlField.setText(existing.getUrl());
            authorField.setText(existing.getAuthor());
            publisherField.setText(existing.getPublisher());
            dateField.setText(NewsPanel.formatDate(existing.getPublishDate()));
            languageBox.setSelectedItem(existing.getLanguage());
            coverField.setText(existing.getCoverImage());
            // Source + URL are the primary key, so they can't be changed.
            sourceField.setEditable(false);
            urlField.setEditable(false);
        }

        // Labels on the left, fields on the right.
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 4));
        form.add(new JLabel("Title *")); form.add(titleField);
        form.add(new JLabel("Description")); form.add(descriptionField);
        form.add(new JLabel("Source *")); form.add(sourceField);
        form.add(new JLabel("URL *")); form.add(urlField);
        form.add(new JLabel("Author")); form.add(authorField);
        form.add(new JLabel("Publisher")); form.add(publisherField);
        form.add(new JLabel("Publish date (yyyy-MM-dd HH:mm:ss)")); form.add(dateField);
        form.add(new JLabel("Language")); form.add(languageBox);
        form.add(new JLabel("Cover image path")); form.add(coverField);

        // Show the form again until the input is valid or the user cancels.
        while (true) {
            int result = JOptionPane.showConfirmDialog(parent, form,
                    editing ? "Edit news" : "Add news", JOptionPane.OK_CANCEL_OPTION);
            if (result != JOptionPane.OK_OPTION) {
                return null;
            }

            String title = titleField.getText().trim();
            String source = sourceField.getText().trim();
            String url = urlField.getText().trim();
            String dateText = dateField.getText().trim();

            // Check the user input before saving anything.
            if (title.isEmpty() || source.isEmpty() || url.isEmpty()) {
                showError(parent, "Title, Source and URL are required.");
                continue;
            }
            Timestamp publishDate = null;
            if (!dateText.isEmpty()) {
                try {
                    publishDate = Timestamp.valueOf(dateText);
                } catch (IllegalArgumentException e) {
                    showError(parent, "The date must look like 2026-10-20 18:30:00");
                    continue;
                }
            }

            // Creation and last modified dates are filled in by the database.
            return new News(title, descriptionField.getText().trim(), source,
                    authorField.getText().trim(), publisherField.getText().trim(), publishDate, url,
                    editing ? existing.getCreationDate() : null, null,
                    (String) languageBox.getSelectedItem(), coverField.getText().trim());
        }
    }

    /** Shows an error message about wrong input. */
    private static void showError(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Invalid input", JOptionPane.WARNING_MESSAGE);
    }
}
