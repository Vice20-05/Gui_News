package DataHandle;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

// Reads the list of RSS feed URLs from the project's JSON configuration file.
public class JsonReader {
    public List<RssSource> readSources(String filePath) throws IOException {
        try (Reader reader = Files.newBufferedReader(Path.of(filePath))) {
            // The JSON file has one root object containing an "rssSources" array.
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray sourcesJson = root.getAsJsonArray("rssSources");

            if (sourcesJson == null) {
                throw new IOException("JSON file does not contain an rssSources array: " + filePath);
            }

            // Convert each object in the array into an RssSource.
            Type sourceListType = new TypeToken<List<RssSource>>() {}.getType();
            List<RssSource> sources = new Gson().fromJson(sourcesJson, sourceListType);
            if (sources == null) {
                throw new IOException("Could not read RSS sources from: " + filePath);
            }

            return sources;
        } catch (JsonParseException | IllegalStateException e) {
            throw new IOException("Invalid RSS source JSON file: " + filePath, e);
        }
    }
}
