package DataHandle;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/** Downloads and safely parses RSS XML documents into News objects. */
public class RssXmlParser {
    /** Downloads the feed from the given URL (for example after a redirect) and parses its news. */
    public List<News> fetchNews(String feedUrl, String sourceName, String language) throws IOException {
        // Check that the text is a real web address before trying to connect.
        URI uri;
        try {
            uri = URI.create(feedUrl);
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid RSS feed URL: " + feedUrl, e);
        }

        // Only allow web addresses, not things like file:// or ftp://.
        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw new IOException("Only HTTP and HTTPS protocols are supported for RSS feeds.");
        }

        URLConnection connection;
        try {
            connection = uri.toURL().openConnection();
        } catch (IOException | IllegalArgumentException e) {
            throw new IOException("Could not open RSS feed URL: " + feedUrl, e);
        }

        // Give up if the server does not answer in time, instead of waiting forever.
        connection.setConnectTimeout(HttpHelper.TIMEOUT_MS);
        connection.setReadTimeout(HttpHelper.TIMEOUT_MS);
        // Some websites block requests that don't say which program is asking.
        connection.setRequestProperty("User-Agent", "GuiNews RSS reader");

        // Download the XML and parse it; try-with-resources closes the stream afterwards.
        try (InputStream feedText = connection.getInputStream()) {
            return parse(feedText, sourceName, language);
        }
    }

    /** Parses RSS {@code item} elements from an input stream. */
    public List<News> parse(InputStream feedText, String sourceName, String language) throws IOException {
        if (feedText == null) {
            throw new IOException("RSS feed input stream cannot be null.");
        }

        List<News> newsList = new ArrayList<>();
        try {
            Document document = createSafeXmlReader().parse(feedText);

            // The publisher is the feed's own name, from <channel><title>.
            String publisher = sourceName;
            NodeList channels = document.getElementsByTagName("channel");
            if (channels.getLength() > 0) {
                String channelTitle = getElementText((Element) channels.item(0), "title");
                if (!channelTitle.isEmpty()) {
                    publisher = HtmlText.toPlainText(channelTitle);
                }
            }

            // Every news article in an RSS feed is inside an <item> tag.
            NodeList items = document.getElementsByTagName("item");
            for (int i = 0; i < items.getLength(); i++) {
                Element item = (Element) items.item(i);
                // Read the tags we need; a missing tag just gives an empty string.
                // RSS feeds often contain HTML (<p>, &#8230;), so we keep only the plain text.
                String title = HtmlText.toPlainText(getElementText(item, "title"));
                String description = HtmlText.toPlainText(getElementText(item, "description"));
                String url = getElementText(item, "link");
                Timestamp publishDate = parseDate(getElementText(item, "pubDate"));

                // WordPress feeds put the author in <dc:creator> instead of <author>.
                String author = getElementText(item, "author");
                if (author.isEmpty()) {
                    author = getElementText(item, "dc:creator");
                }

                // Creation and last modified dates are set by the database when the item is saved.
                newsList.add(new News(title, description, sourceName, author, publisher,
                        publishDate, url, null, null, language, null));
            }
        } catch (Exception e) {
            // Broken XML ends up here, so one bad feed doesn't crash the program.
            throw new IOException("Error parsing RSS feed: " + e.getMessage(), e);
        }
        return newsList;
    }

    /** Turns an RSS date like "Mon, 20 Oct 2026 18:30:00 +0000" into a Timestamp, or null if it is invalid. */
    private Timestamp parseDate(String text) {
        try {
            ZonedDateTime date = ZonedDateTime.parse(text, DateTimeFormatter.RFC_1123_DATE_TIME);
            return Timestamp.from(date.toInstant());
        } catch (DateTimeParseException e) {
            // A wrong or missing date should not stop the whole feed.
            return null;
        }
    }

    /** Creates an XML reader with external entity access disabled. */
    private DocumentBuilder createSafeXmlReader() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // These settings stop the XML from loading other files or websites on its own
        // (an "XXE" attack), because the feed comes from the internet and can't be trusted.
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory.newDocumentBuilder();
    }

    /** Returns the trimmed text of the first matching child element. */
    private String getElementText(Element parent, String tagName) {
        NodeList elements = parent.getElementsByTagName(tagName);
        // Not every feed has every tag (for example <author>), so return "" instead of crashing.
        if (elements.getLength() == 0) {
            return "";
        }
        return elements.item(0).getTextContent().trim();
    }
}
