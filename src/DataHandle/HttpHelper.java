package DataHandle;

import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URI;

/** Sends HTTP GET requests to check whether a URL can be reached. */
public class HttpHelper {
    // How long to wait for a server before giving up (5 seconds).
    public static final int TIMEOUT_MS = 5000;

    /** Returned when the request timed out. */
    public static final int TIMEOUT = -2;

    /** Returned when there is no connection or the URL is invalid. */
    public static final int NO_CONNECTION = -1;

    /** Sends a GET request and returns the HTTP status code, or TIMEOUT / NO_CONNECTION. */
    public static int getStatusCode(String urlAddress) {
        HttpURLConnection connection = null;
        try {
            connection = openGet(urlAddress);
            return connection.getResponseCode();
        } catch (SocketTimeoutException e) {
            return TIMEOUT;
        } catch (Exception e) {
            return NO_CONNECTION;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /** Returns the address a 301/302 redirect points to, or null if there is none. */
    public static String getRedirectLocation(String urlAddress) {
        HttpURLConnection connection = null;
        try {
            connection = openGet(urlAddress);
            String location = connection.getHeaderField("Location");
            if (location == null) {
                return null;
            }
            // The Location header can be relative ("/feed/"), so resolve it against the original URL.
            return URI.create(urlAddress).resolve(location).toString();
        } catch (Exception e) {
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /** Returns true for the redirect status codes. */
    public static boolean isRedirect(int statusCode) {
        return statusCode == 301 || statusCode == 302 || statusCode == 307 || statusCode == 308;
    }

    /** Explains a status code in a short, readable message. */
    public static String describeStatus(int statusCode) {
        return switch (statusCode) {
            case 200 -> "200 OK";
            case 301 -> "301 Moved Permanently (redirect)";
            case 302 -> "302 Found (redirect)";
            case 307 -> "307 Temporary Redirect";
            case 308 -> "308 Permanent Redirect";
            case 400 -> "400 Bad Request";
            case 401 -> "401 Unauthorized";
            case 403 -> "403 Forbidden";
            case 404 -> "404 Not Found";
            case 500 -> "500 Internal Server Error";
            case TIMEOUT -> "timeout after " + (TIMEOUT_MS / 1000) + " seconds";
            case NO_CONNECTION -> "no connection";
            default -> "HTTP " + statusCode;
        };
    }

    /** Prepares a GET request with our timeouts and settings (it is sent when we read the answer). */
    private static HttpURLConnection openGet(String urlAddress) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) URI.create(urlAddress).toURL().openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(TIMEOUT_MS);
        connection.setReadTimeout(TIMEOUT_MS);
        // Don't follow redirects automatically, so we can see the 301/302 codes ourselves.
        connection.setInstanceFollowRedirects(false);
        // Some websites answer 403 to requests without a User-Agent.
        connection.setRequestProperty("User-Agent", "GuiNews RSS reader");
        return connection;
    }
}
