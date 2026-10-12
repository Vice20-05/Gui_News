package DataHandle;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/** Downloads and safely parses RSS XML documents into article objects. */
public class RssXmlParser {
    /** Downloads one HTTP or HTTPS feed and parses its articles. */
    public List<RssArticle> fetchArticles(RssSource source) throws IOException {
        if (source == null || source.getUrl() == null || source.getUrl().isBlank()) {
            throw new IOException("RSS source must contain a URL.");
        }
        return fetchArticles(source.getUrl(), source.getName(), source.getLanguage());
    }

    /** Downloads the feed from the given URL (for example after a redirect) and parses its articles. */
    public List<RssArticle> fetchArticles(String feedUrl, String sourceName, String language) throws IOException {
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

    /** @deprecated use {@link #fetchArticles(RssSource)} */
    @Deprecated
    public List<RssArticle> fetchArticales(RssSource source) throws IOException {
        return fetchArticles(source);
    }

    /** Parses RSS {@code item} elements from an input stream. */
    public List<RssArticle> parse(InputStream feedText, String sourceName, String language) throws IOException {
        if (feedText == null) {
            throw new IOException("RSS feed input stream cannot be null.");
        }

        List<RssArticle> articles = new ArrayList<>();
        try {
            Document document = createSafeXmlReader().parse(feedText);

            // Every news article in an RSS feed is inside an <item> tag.
            NodeList items = document.getElementsByTagName("item");
            for (int i = 0; i < items.getLength(); i++) {
                Element item = (Element) items.item(i);
                // Read the tags we need; a missing tag just gives an empty string.
                String title = getElementText(item, "title");
                String description = getElementText(item, "description");
                String url = getElementText(item, "link");
                String publishDate = getElementText(item, "pubDate");
                articles.add(new RssArticle(title, description, url, publishDate, language, sourceName));
            }
        } catch (Exception e) {
            // Broken XML ends up here, so one bad feed doesn't crash the program.
            throw new IOException("Error parsing RSS feed: " + e.getMessage(), e);
        }
        return articles;
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
