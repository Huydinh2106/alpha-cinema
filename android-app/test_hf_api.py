#!/usr/bin/env python3
import json
import sys
import urllib.error
import urllib.request
import uuid


BASE_URL = "https://vankhoa2110-rag-alphacinema.hf.space"
GENERATION_MODEL = "gpt-4o-mini"

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")
if hasattr(sys.stderr, "reconfigure"):
    sys.stderr.reconfigure(encoding="utf-8")


def request_json(method, path, payload=None, timeout=90):
    url = f"{BASE_URL}{path}"
    body = None
    headers = {"Accept": "application/json"}

    if payload is not None:
        body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        headers["Content-Type"] = "application/json"

    request = urllib.request.Request(
        url=url,
        data=body,
        headers=headers,
        method=method,
    )

    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            raw = response.read().decode("utf-8")
            try:
                parsed = json.loads(raw) if raw else {}
            except json.JSONDecodeError:
                parsed = {"raw": raw}
            return response.status, parsed
    except urllib.error.HTTPError as exc:
        raw = exc.read().decode("utf-8", errors="replace")
        try:
            parsed = json.loads(raw) if raw else {}
        except json.JSONDecodeError:
            parsed = {"raw": raw}
        return exc.code, parsed


def print_section(title):
    print(f"\n{'=' * 12} {title} {'=' * 12}")


def print_chat_response(data):
    print(f"intent: {data.get('intent')}")
    print(f"mode: {data.get('mode')}")
    print(f"session_id: {data.get('session_id')}")
    print(f"history_message_count: {data.get('history_message_count')}")
    print("\nanswer:")
    print(data.get("answer") or "")

    recommendations = data.get("recommendations") or []
    print_recommendations(recommendations)


def print_recommendations(recommendations):
    print(f"\nrecommendations ({len(recommendations)}):")
    for index, item in enumerate(recommendations, start=1):
        title = item.get("title") or item.get("name") or item.get("movie_title") or "Untitled"
        slug = item.get("slug") or item.get("movie_slug") or item.get("id") or ""
        year = item.get("year") or ""
        reasons = item.get("why_recommended") or item.get("reasons") or []
        year_text = f"({year})" if year else ""
        slug_text = f"- {slug}" if slug else ""
        print(f"{index}. {title} {year_text} {slug_text}".strip())
        if reasons:
            print(f"   reasons: {', '.join(reasons)}")


def build_ask_payload(question, session_id, chat_history=None):
    return {
        "question": question,
        "top_k": 6,
        "top_n_recommendations": 5,
        "generation_model": GENERATION_MODEL,
        "session_id": session_id,
        "remember_history": True,
        "chat_history": chat_history or [],
    }


def main():
    session_id = f"test-{uuid.uuid4()}"

    print_section("Health check")
    status, health = request_json("GET", "/")
    print(f"GET / status: {status}")
    print(json.dumps(health, ensure_ascii=False, indent=2))
    if status >= 400:
        return 1

    print_section("POST /ask")
    first_question = "Gợi ý cho tôi vài phim hành động đang đáng xem."
    first_payload = build_ask_payload(
        question=first_question,
        session_id=session_id,
    )
    status, first_response = request_json("POST", "/ask", first_payload)
    print(f"POST /ask status: {status}")
    print_chat_response(first_response)
    if status >= 400:
        return 1

    print_section("POST /ask multi-turn")
    second_question = "Trong các phim đó, phim nào hợp xem cùng bạn bè nhất?"
    chat_history = [
        {"role": "user", "content": first_question},
        {"role": "assistant", "content": first_response.get("answer", "")},
    ]
    second_payload = build_ask_payload(
        question=second_question,
        session_id=first_response.get("session_id") or session_id,
        chat_history=chat_history,
    )
    status, second_response = request_json("POST", "/ask", second_payload)
    print(f"POST /ask multi-turn status: {status}")
    print_chat_response(second_response)
    if status >= 400:
        return 1

    print_section("POST /recommend")
    recommend_payload = {
        "query": "phim kinh dị hoặc giật gân cho buổi tối",
        "top_n": 5,
    }
    status, recommend_response = request_json("POST", "/recommend", recommend_payload)
    print(f"POST /recommend status: {status}")
    print_recommendations(recommend_response.get("recommendations", []))
    if status >= 400:
        return 1

    print_section("Cleanup")
    cleanup_session_id = second_response.get("session_id") or first_response.get("session_id") or session_id
    status, cleanup_response = request_json("DELETE", f"/sessions/{cleanup_session_id}")
    print(f"DELETE /sessions/{cleanup_session_id} status: {status}")
    print(json.dumps(cleanup_response, ensure_ascii=False, indent=2))

    return 0


if __name__ == "__main__":
    sys.exit(main())
