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

### Backend & System
- API server xử lý dữ liệu
- Worker xử lý background jobs
- Kiến trúc scalable

## Cấu trúc dự án

```
alpha-cinema/
│
├── android-app/     # Mobile app (Android)
├── backend/         # API Server
├── worker/          # Background jobs
├── infra/           # Infrastructure & deployment
├── docs/            # Documentation
│
├── docker-compose.yml
├── .env.example
└── README.md
```

## Công nghệ sử dụng

### Backend
- Node.js / NestJS
- REST API / GraphQL
- JWT Authentication

### Mobile
- Android (Kotlin / Java)

### AI Chatbot
- OpenAI API / LLM
- NLP (Natural Language Processing)
- Recommendation system

### DevOps
- Docker
- Docker Compose
- GitHub Actions (CI/CD)

## Chatbot hoạt động

Ví dụ:

> Người dùng: "Gợi ý phim giống John Wick"

Hệ thống:
1. Phân tích yêu cầu (NLP)
2. Truy vấn dữ liệu phim
3. Kết hợp AI hiểu ngữ cảnh
4. Trả về kết quả phù hợp

## Cài đặt & chạy

### 1. Clone repo

```bash
git clone https://github.com/your-username/alpha-cinema.git
cd alpha-cinema
```
### 2. Cấu hình môi trường

```bash
cp .env.example .env
```

Cập nhật:
- Database URL
- API Keys (OpenAI,...)

### 3. Chạy Docker

```bash
docker-compose up --build
```

### 4. Truy cập

- Backend: http://localhost:3000

## 📡 API mẫu

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
