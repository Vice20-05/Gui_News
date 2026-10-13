import java.awt.Component;
import java.awt.GridLayout;
import java.sql.Timestamp;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import DataHandle.Event;
import DataHandle.News;

/** A pop-up form for adding or editing a news item or an event. */
public class NewsForm {
    /**
     * Shows the form. Pass null to add a new item, or an existing item to edit it.
     * With isEvent = true the form also asks for the event time and location and returns an Event.
     * Returns the filled-in News (or Event), or null if the user pressed Cancel.
     */
    public static News show(Component parent, News existing, boolean isEvent) {
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
        // Only used for events.
        JTextField eventDateField = new JTextField(30);
        JTextField locationField = new JTextField(30);

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
            if (existing instanceof Event event) {
                eventDateField.setText(NewsPanel.formatDate(event.getEventDateTime()));
                locationField.setText(event.getLocation());
            } else {
                // Source + URL are the primary key of news, so they can't be changed.
                // (Events have a different key, so for them these stay editable.)
                sourceField.setEditable(false);
                urlField.setEditable(false);
            }
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
        if (isEvent) {
            form.add(new JLabel("Event start (yyyy-MM-dd HH:mm:ss) *")); form.add(eventDateField);
            form.add(new JLabel("Location *")); form.add(locationField);
        }

        // Show the form again until the input is valid or the user cancels.
        while (true) {
            int result = JOptionPane.showConfirmDialog(parent, form,
                    (editing ? "Edit " : "Add ") + (isEvent ? "event" : "news"), JOptionPane.OK_CANCEL_OPTION);
            if (result != JOptionPane.OK_OPTION) {
                return null;
            }

            String title = titleField.getText().trim();
            String source = sourceField.getText().trim();
            String url = urlField.getText().trim();
            String dateText = dateField.getText().trim();

            // Check the user input before saving anything.
            // Events don't need a source and URL, because those are not part of their key.
            if (title.isEmpty() || (!isEvent && (source.isEmpty() || url.isEmpty()))) {
                showError(parent, isEvent ? "Title is required." : "Title, Source and URL are required.");
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

            if (isEvent) {
                String location = locationField.getText().trim();
                Timestamp eventDateTime;
                try {
                    eventDateTime = Timestamp.valueOf(eventDateField.getText().trim());
                } catch (IllegalArgumentException e) {
                    showError(parent, "The event start must look like 2026-10-20 18:30:00");
                    continue;
                }
                if (location.isEmpty()) {
                    showError(parent, "Location is required.");
                    continue;
                }
                return new Event(title, descriptionField.getText().trim(), source,
                        authorField.getText().trim(), publisherField.getText().trim(), publishDate, url,
                        editing ? existing.getCreationDate() : null, null,
                        (String) languageBox.getSelectedItem(), coverField.getText().trim(),
                        eventDateTime, location);
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
