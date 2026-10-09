package Database;

import DataHandle.RssArticle;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

// Downloads an RSS feed over HTTPS and turns its XML into RssArticle objects.
//
// An RSS feed is an XML file that looks roughly like this:
//
//   <rss>
//     <channel>
//       <title>Site name</title>
//       <language>ro-RO</language>          <- language key for every article
//       <item>                              <- one <item> = one article
//         <title>Article title</title>
//         <link>https://site/article</link>
//         <description>Short text</description>
//         <pubDate>Mon, 05 Oct 2026 10:00:00 +0000</pubDate>
//       </item>
//       ...more <item> elements...
//     </channel>
//   </rss>
public class RssXmlParser {
    // One HttpClient can be reused for every feed, so we create it only once.
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    // Step 1: download the feed. Step 2: parse the XML.
    public List<RssArticle> fetchArticles(String feedUrl) throws IOException {
        if (!feedUrl.startsWith("https://")) {
            throw new IOException("Only https:// feeds are allowed: " + feedUrl);
        }

        HttpRequest request = HttpRequest.newBuilder(URI.create(feedUrl))
                .timeout(Duration.ofSeconds(20))
                // Some websites block requests that do not say who they are.
                .header("User-Agent", "GuiNews RSS reader")
                .GET()
                .build();

        try {
            HttpResponse<InputStream> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            // 200 means "OK". Anything else (404, 500, ...) means the download failed.
            if (response.statusCode() != 200) {
                response.body().close();
                throw new IOException("Feed returned HTTP " + response.statusCode() + ": " + feedUrl);
            }
            try (InputStream body = response.body()) {
                return parse(body);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Download was interrupted: " + feedUrl, e);
        }
    }

    // Reads RSS XML from any stream (a download, or a local file when testing).
    public List<RssArticle> parse(InputStream xml) throws IOException {
        try {
            Document document = createSafeBuilder().parse(xml);

            // The <language> tag sits once in <channel> and applies to all articles.
            String language = firstText(document.getDocumentElement(), "language");

            List<RssArticle> articles = new ArrayList<>();
            NodeList items = document.getElementsByTagName("item");
            for (int i = 0; i < items.getLength(); i++) {
                Element item = (Element) items.item(i);
                articles.add(new RssArticle(
                        firstText(item, "title"),
                        removeHtml(firstText(item, "description")),
                        firstText(item, "link"),
                        firstText(item, "pubDate"),
                        language
                ));
            }
            return articles;
        } catch (SAXException e) {
            throw new IOException("Feed is not valid XML", e);
        }
    }

    // Builds an XML reader that refuses DOCTYPE declarations.
    // Without this, a malicious feed could make the parser read files from your computer
    // (this attack is called "XXE"). Normal RSS feeds never need a DOCTYPE.
    private DocumentBuilder createSafeBuilder() throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            return factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new IOException("Could not create the XML parser", e);
        }
    }

    // Returns the text of the first child tag with this name, or "" if it is missing.
    private String firstText(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) {
            return "";
        }
        Node node = nodes.item(0);
        return node.getTextContent().trim();
    }

    // Descriptions often contain HTML like <p> or <img>. We keep only the plain text.
    private String removeHtml(String text) {
        return text.replaceAll("<[^>]*>", "")
                .replace("&nbsp;", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
