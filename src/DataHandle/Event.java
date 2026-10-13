package DataHandle;

import java.sql.Timestamp;


//The Event doesn't need to be linked with the gui 
/**
 * An event: has every field of a news item, plus when and where the event takes place.
 * Because it extends News, an Event can be shown in the same list and details panel as news.
 */
public class Event extends News {
    // Day and start time of the event in one value, e.g. 2026-10-20 18:30:00.
    private Timestamp eventDateTime;
    // Where the event takes place, e.g. "Culture Palace, Targu Mures".
    private String location;

    // title + eventDateTime + location together are the primary key in the events table.
    // Changing any of them changes the key, so updateEvent needs the old values to find the row.

    /**
     * Creates an event. The first eleven parameters are the same as for News;
     * a missing location becomes "" and a missing cover becomes the default image.
     */
    public Event(String title, String description, String source, String author, String publisher,
                 Timestamp publishDate, String url, Timestamp creationDate, Timestamp lastModifiedDate,
                 String language, String coverImage, Timestamp eventDateTime, String location) {
        // Let News store the shared fields (and replace nulls with "").
        super(title, description, source, author, publisher, publishDate, url,
                creationDate, lastModifiedDate, language, coverImage);
        this.eventDateTime = eventDateTime;
        this.location = location == null ? "" : location;
    }

    public Timestamp getEventDateTime() { return eventDateTime; }
    public void setEventDateTime(Timestamp eventDateTime) { this.eventDateTime = eventDateTime; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location == null ? "" : location; }
}
