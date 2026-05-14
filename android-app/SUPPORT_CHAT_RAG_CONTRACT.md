# Support Chat RAG Contract

Backend:

- Base URL: `https://vankhoa2110-rag-alphacinema.hf.space`
- Health check: `GET /`
- Chat: `POST /ask`
- Recommendation shortcut: `POST /recommend`
- Clear session: `DELETE /sessions/{session_id}`

## `POST /ask`

Request:

```json
{
  "question": "Goi y cho toi vai phim hanh dong dang xem.",
  "top_k": 6,
  "top_n_recommendations": 5,
  "generation_model": "gpt-4o-mini",
  "session_id": "session-123",
  "remember_history": true,
  "chat_history": [
    {
      "role": "user",
      "content": "Toi muon xem phim hanh dong."
    },
    {
      "role": "assistant",
      "content": "Ban co the xem mot so phim hanh dong noi bat."
    }
  ]
}
```

Notes:

- `question` is required.
- `top_k` defaults to `6` on the backend and must stay between `1` and `20`.
- `top_n_recommendations` defaults to `5` on the backend and must stay between `1` and `10`.
- `generation_model` defaults to `gpt-4o-mini`.
- `session_id` is optional for the backend, but the Android app sends one for multi-turn memory.
- `chat_history` is optional and should contain previous turns only; the current user message is sent in `question`.

Response:

```json
{
  "intent": "policy",
  "mode": "answer",
  "answer": "Noi dung tra loi cua tro ly.",
  "sources": [],
  "recommendations": [],
  "session_id": "session-123",
  "history_message_count": 2
}
```

Supported `intent` values:

- `policy`
- `movie`
- `recommendation`
- `mixed`
- `music_request`

## Spotify music search

Music requests such as `Mo giao dien album nhac phim Interstellar` are handled locally first so the chatbot can render a Spotify card immediately.

Flow:

1. Android extracts the target title and builds a query like `Interstellar soundtrack`.
2. `SpotifyRepository` requests a Client Credentials access token from `https://accounts.spotify.com/api/token`.
3. It calls `GET https://api.spotify.com/v1/search?q={query}&type=album&limit=1`.
4. The first album result is rendered as a chatbot link card with `id`, `uri`, album art, and `external_urls.spotify`.
5. If credentials are missing or Spotify fails, the app returns no Spotify link card and tells the user the album could not be resolved through Spotify API.

Configure these values through Gradle properties, environment variables, or `local.properties`:

```properties
SPOTIFY_CLIENT_ID=...
SPOTIFY_CLIENT_SECRET=...
```

For production, keep the Spotify client secret on the backend and return the resolved album payload to Android instead of shipping the secret in the APK.

## `POST /recommend`

Request:

```json
{
  "query": "phim kinh di hoac giat gan cho buoi toi",
  "top_n": 5
}
```

Response:

```json
{
  "recommendations": [
    {
      "title": "Movie title",
      "slug": "movie-slug",
      "year": 2026,
      "poster_url": "https://example.com/poster.jpg"
    }
  ]
}
```

## Current Android Behavior

- `SupportViewModel` sends the current message as `question`.
- Previous turns are sent as `chat_history`.
- The app keeps a session id in `SettingsManager` and sends it to `/ask`.
- `SupportRepository` calls `POST /ask` through Retrofit and falls back to local suggestions if the server is unavailable.
- `SupportChatResponseParser` reads `answer`, `intent`, `session_id`, `history_message_count`, and movie items from `recommendations`.
- The app renders movie cards only when recommendations exist or when the request is clearly recommendation-like.
