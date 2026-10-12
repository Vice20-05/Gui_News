package DataHandle;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Turns the HTML found in RSS fields into plain text for the UI. */
public class HtmlText {
    private static final Pattern ENTITY = Pattern.compile("&(#[0-9]+|#[xX][0-9a-fA-F]+|[a-zA-Z]+);");

    private static final Map<String, String> NAMED_ENTITIES = Map.ofEntries(
            Map.entry("amp", "&"), Map.entry("lt", "<"), Map.entry("gt", ">"),
            Map.entry("quot", "\""), Map.entry("apos", "'"), Map.entry("nbsp", " "),
            Map.entry("hellip", "…"), Map.entry("ndash", "–"), Map.entry("mdash", "—"),
            Map.entry("lsquo", "‘"), Map.entry("rsquo", "’"),
            Map.entry("ldquo", "“"), Map.entry("rdquo", "”"),
            Map.entry("bdquo", "„"), Map.entry("laquo", "«"), Map.entry("raquo", "»")
    );

    /** Removes HTML tags and entities, keeping paragraphs as separate lines. */
    public static String toPlainText(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }

        String text = html
                // Paragraphs and line breaks become new lines before the tags are removed.
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</p>", "\n\n")
                .replaceAll("<[^>]*>", "");

        text = decodeEntities(text);

        // WordPress feeds add "The post X appeared first on Y." at the end, which is just noise.
        text = text.replaceAll("(?m)^The post .* appeared first on .*$", "");

        // Collapse the extra spaces and empty lines left over from the removed tags.
        return text
                .replace('\u00A0', ' ')
                .replaceAll("[ \\t]+", " ")
                .replaceAll(" *\\n *", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    /** Replaces HTML entities (for example "&amp;" or "&#8230;") with the characters they stand for. */
    public static String decodeEntities(String text) {
        if (text == null || text.indexOf('&') < 0) {
            return text == null ? "" : text;
        }

        Matcher matcher = ENTITY.matcher(text);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String name = matcher.group(1);
            String replacement;
            try {
                if (name.startsWith("#x") || name.startsWith("#X")) {
                    replacement = Character.toString(Integer.parseInt(name.substring(2), 16));
                } else if (name.startsWith("#")) {
                    replacement = Character.toString(Integer.parseInt(name.substring(1)));
                } else {
                    // Unknown named entities are left unchanged.
                    replacement = NAMED_ENTITIES.getOrDefault(name, matcher.group());
                }
            } catch (IllegalArgumentException e) {
                // Invalid numbers (too large, not a valid character) are left unchanged.
                replacement = matcher.group();
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
