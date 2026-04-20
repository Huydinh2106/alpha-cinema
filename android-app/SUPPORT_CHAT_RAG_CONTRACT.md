# Support Chat RAG Contract

This app now sends a backend-ready payload for short-term conversation memory.

## Endpoint

- `POST /ask`

## Request shape

```json
{
  "question": "Gợi ý cho tôi một bộ phim kinh dị đi",
  "top_k": 4,
  "session_id": "8fcbdb49-3d83-4aa2-9a5c-24d6e9800f7b",
  "history": [
    {
      "role": "assistant",
      "content": "Xin chào, tôi là trợ lý AlphaCinema.",
      "timestamp": "20:10"
    },
    {
      "role": "user",
      "content": "Gợi ý cho tôi một bộ phim kinh dị đi",
      "timestamp": "20:11"
    }
  ],
  "memory": {
    "summary": "User is asking for horror movie suggestions.",
    "last_intent": "movie_recommendation",
    "topics": ["movies"],
    "genres": ["Kinh dị"],
    "referenced_movie_slugs": [],
    "referenced_movie_titles": [],
    "kids_mode_enabled": false,
    "surface": "support_chat"
  },
  "response_contract": {
    "version": "2026-04-12",
    "include_intent": true,
    "structured_movies_only_for_recommendation": true,
    "required_movie_fields": ["id", "title", "slug", "actions"],
    "supported_action_types": [
      "play_movie",
      "view_detail",
      "save_to_list",
      "watch_trailer"
    ],
    "rules": [
      "Return movies only when the user explicitly asks for movie recommendations or what-to-watch suggestions.",
      "For app policy, account, troubleshooting, or general support questions return text-only with no movies array.",
      "When intent is movie_recommendation include intent and structured movie actions so the app can render CTA buttons."
    ]
  }
}
```

## Response shape

### Non-recommendation response

```json
{
  "intent": "general_support",
  "answer": "AlphaCinema không cho phép chia sẻ tài khoản cho nhiều người cùng lúc.",
  "memory": {
    "summary": "User asked about app rules and account usage.",
    "last_intent": "general_support",
    "topics": ["app_policy", "account"],
    "genres": [],
    "referenced_movie_slugs": [],
    "referenced_movie_titles": [],
    "surface": "support_chat"
  }
}
```

### Recommendation response

```json
{
  "intent": "movie_recommendation",
  "answer": "Bạn có thể xem: Úng Kính Ma Quái (2026)",
  "movies": [
    {
      "id": "ung-kinh-ma-quai-2026",
      "title": "Úng Kính Ma Quái",
      "subtitle": "Haunted Lens",
      "year": 2026,
      "poster_url": "https://phimimg.com/poster.jpg",
      "slug": "ung-kinh-ma-quai-2026",
      "playable": true,
      "actions": [
        {
          "type": "play_movie",
          "label": "Xem phim",
          "route": {
            "destination": "player",
            "slug": "ung-kinh-ma-quai-2026"
          },
          "fallback_route": {
            "destination": "detail",
            "slug": "ung-kinh-ma-quai-2026"
          }
        }
      ]
    }
  ],
  "memory": {
    "summary": "User prefers horror suggestions.",
    "last_intent": "movie_recommendation",
    "topics": ["movies"],
    "genres": ["Kinh dị"],
    "referenced_movie_slugs": ["ung-kinh-ma-quai-2026"],
    "referenced_movie_titles": ["Úng Kính Ma Quái"],
    "surface": "support_chat"
  }
}
```

## Backend rules

1. Always read `session_id`, `history`, and `memory` together.
2. `memory.summary` should be a compact rolling summary for the session.
3. For non-recommendation intents, return text-only and omit `movies`.
4. For recommendation intents, return `intent = "movie_recommendation"` and structured `movies`.
5. If playback is not available, set the movie or action so the app can fall back to detail.

## Current app behavior

- The app keeps a per-session `session_id`.
- The app sends the last 8 turns in `history`.
- The app merges local memory cues with any `memory` object returned by backend.
- The app only renders `Xem phim` CTA if the question is recommendation-like or backend sets `intent = movie_recommendation`.
