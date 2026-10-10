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

public class RssXmlParser {
    public List<RssArticle> fetchArticales(RssSource source) throws IOException {
        if (source == null || source.getUrl() == null || source.getUrl().isBlank()) {
            throw new IOException("RSS source must contain a URL.");
        }

        String feedUrl = source.getUrl();
        URI uri;
        try {
            uri = URI.create(feedUrl);
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid RSS feed URL: " + feedUrl, e);
        }

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

        connection.setConnectTimeout(10000);
        connection.setReadTimeout(20000);
        connection.setRequestProperty("User-Agent", "GuiNews RSS reader");

        try (InputStream feedText = connection.getInputStream()) {
            return parse(feedText, source.getName());
        }
    }

    public List<RssArticle> parse(InputStream feedText, String sourceName) throws IOException {
        if (feedText == null) {
            throw new IOException("RSS feed input stream cannot be null.");
        }

        List<RssArticle> articles = new ArrayList<>();
        try {
            Document document = createSafeXmlReader().parse(feedText);
            NodeList items = document.getElementsByTagName("item");
            for (int i = 0; i < items.getLength(); i++) {
                Element item = (Element) items.item(i);
                String title = getElementText(item, "title");
                String description = getElementText(item, "description");
                String url = getElementText(item, "link");
                String publishDate = getElementText(item, "pubDate");
                articles.add(new RssArticle(title, description, url, publishDate, "", sourceName));
            }
        } catch (Exception e) {
            throw new IOException("Error parsing RSS feed: " + e.getMessage(), e);
        }
        return articles;
    }

    private DocumentBuilder createSafeXmlReader() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
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

    private String getElementText(Element parent, String tagName) {
        NodeList elements = parent.getElementsByTagName(tagName);
        if (elements.getLength() == 0) {
            return "";
        }
        return elements.item(0).getTextContent().trim();
    }
}
