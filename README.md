# Alpha Cinema

**Alpha Cinema** là ứng dụng xem phim trên nền tảng Android tích hợp **AI Chatbot thông minh**, giúp người dùng tìm kiếm, khám phá và nhận gợi ý phim thông qua hội thoại tự nhiên.

## Tính năng nổi bật

### Ứng dụng Android
- Giao diện thân thiện, tối ưu mobile
- Xem phim chất lượng cao
- Tìm kiếm và lọc phim nhanh chóng

### AI Chatbot
- Gợi ý phim theo sở thích
- Trả lời câu hỏi về phim
- Tương tác hội thoại tự nhiên
- Cá nhân hóa trải nghiệm

```

## Công nghệ sử dụng

### Mobile
- Android (Kotlin / Java)

### AI Chatbot
- OpenAI API / LLM
- NLP (Natural Language Processing)
- Recommendation system

## Chatbot hoạt động

Ví dụ:

> Người dùng: "Gợi ý phim giống John Wick"

Hệ thống:
1. Phân tích yêu cầu (NLP)
2. Truy vấn dữ liệu phim
3. Kết hợp AI hiểu ngữ cảnh
4. Trả về kết quả phù hợp

## Cài đặt & chạy

### Clone repo

```bash
git clone https://github.com/Huydinh2106/alpha-cinema.git
cd alpha-cinema
```
### Get movies

```http
GET /api/movies
```

### Chat AI

```http
POST /api/chat
Content-Type: application/json

{
  "message": "Gợi ý phim hay"
}
```

## Roadmap

- Recommendation AI nâng cao
- Web / iOS version
- Real-time chat (WebSocket)
- Social features (rating, comment)

## Contributing

```
fork → branch → commit → pull request
```

## 📄 License

This project is proprietary.

All rights reserved © Alpha Team.

## 👨‍💻 Author

Alpha Team 
