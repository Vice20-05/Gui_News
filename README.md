# News & Events Manager

A Java Swing desktop app for managing news and events. It loads news from RSS feeds and stores everything in a local SQLite database.

## Features

- Add, view, edit and delete news and events
- Load news from RSS feeds (Romanian and Hungarian sources)
- Search by title and description
- Filter by language (Romanian / Hungarian / Both)
- Refresh button that loads in the background, with a loading bar
- Clear DB button that asks for confirmation
- Default cover image when a news item has no image of its own
- Dark theme (black background, white text)

## Technologies

- Java Swing
- SQLite
- RSS / XML parsing
- HTTP GET
- JSON (Gson)
- Maven

## Project structure

```
src/
├── Main.java           # Starts the app and applies the dark theme
├── GUI.java            # Main window and button actions
├── NewsPanel.java      # List (left) and details panel (right)
├── NewsForm.java       # Add / edit form
└── DataHandle/
    ├── News.java           # News model
    ├── Event.java          # Event model (extends News)
    ├── NewsDatabase.java   # SQLite CRUD operations
    ├── RssSource.java      # One RSS source from rss.json
    ├── JsonReader.java     # Reads rss.json
    ├── HttpHelper.java     # HTTP GET with timeout and status codes
    ├── RssXmlParser.java   # Parses RSS XML into News objects
    ├── RssUpdater.java     # Downloads feeds and saves the news
    └── HtmlText.java       # Removes HTML tags from the descriptions
res/
├── rss.json            # RSS feed list
└── images/
    └── default.jpg     # Default cover image
```

## How to run

1. Install Java 17 or newer.
2. Open the project in VS Code or IntelliJ.
3. Let Maven download the dependencies (Gson, SQLite JDBC).
4. Run `Main.java`.

The database (`news.db`) is created automatically in the project folder.

## Database

- `news` table: composite primary key `(source, url)`
- `events` table: composite primary key `(title, event_datetime, location)`

## RSS sources

The RSS feeds are listed in `res/rss.json`, not hardcoded in the source code. To add a source, add a new entry:

```json
{"name": "Source Name", "url": "https://example.com/feed/", "language": "RO"}
```
