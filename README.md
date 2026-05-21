# Alpha Cinema

**Languages:** English | [Tiếng Việt](#tieng-viet)

Alpha Cinema is a modern Android movie streaming application built with Kotlin and Jetpack Compose. The app combines movie discovery, high-quality playback, AI-assisted recommendations, real-time watch parties, subscription payments, and Firebase-backed user features.

## Highlights

- Browse, search, filter, and stream movies with a mobile-first Android experience.
- Discover movies through an AI support chatbot with natural-language recommendations.
- Create and join Watch Party rooms with synchronized playback, text chat, voice communication, and deep-link support.
- Manage user accounts with Firebase Authentication, Google Sign-In, profiles, favorites, watch history, ratings, and comments.
- Support paid subscription plans through MoMo payments, verified by Firebase Cloud Functions.
- Send push notifications for new movies, recommendations, and subscription reminders.
- Provide admin tools for importing movies, managing home categories, assigning age ratings, and supporting kids-friendly content.

## Tech Stack

- **Mobile:** Kotlin, Jetpack Compose, Material 3, Navigation Compose, Coroutines, Flow
- **Media:** Media3/ExoPlayer, HLS, Coil, Lottie
- **Backend:** Firebase Auth, Firestore, Realtime Database, Storage, Cloud Messaging, Cloud Functions
- **Integrations:** MoMo SDK, Agora Voice SDK, Retrofit, OkHttp, TMDB API, EmailJS
- **Functions:** Node.js 20, TypeScript, Firebase Admin SDK

## Project Structure

```text
.
|-- android-app/        # Android application source code
|-- functions/          # Firebase Cloud Functions for payments, auth helpers, and notifications
|-- scripts/            # Utility scripts for data population
|-- firestore.rules     # Firestore security rules
|-- firebase.json       # Firebase project configuration
`-- README.md
```

## Core Features

### Movie Streaming

- Home, search, category, movie list, and movie detail screens
- Episode selection for multi-episode content
- HLS playback using Media3/ExoPlayer
- Watch progress tracking and playback history

### AI Movie Assistant

- Natural-language movie Q&A and recommendations
- Context-aware follow-up suggestions
- Fallback recommendation logic from the movie catalog
- In-app navigation actions from chatbot suggestions

### Watch Party

- Room creation and joining with short room codes
- Real-time synchronized playback state
- Real-time member list and text chat
- Voice communication using Agora Voice SDK
- Deep link support with `alphacinema://watchparty/{roomId}`

### Account and Engagement

- Email/password authentication and Google Sign-In
- Profile management
- Favorites, ratings, comments, and watch history
- Push notifications through Firebase Cloud Messaging

### Payments and Subscriptions

- MoMo payment and MoMo app payment flows
- Server-side payment creation and verification through Cloud Functions
- Subscription status synchronization with Firestore

### Admin Tools

- Search and import movies from an external movie API
- Store normalized movie data in Firestore
- Manage home categories
- Auto-evaluate age rating and kids-friendly status

## Getting Started

### Prerequisites

- Android Studio
- JDK 17 or newer
- Node.js 20 for Firebase Cloud Functions
- A Firebase project with Android app configuration
- Firebase CLI access via `npx -y firebase-tools@latest`

### Android App

1. Clone the repository:

```bash
git clone https://github.com/Huydinh2106/alpha-cinema.git
cd alpha-cinema
```

2. Add your Firebase Android configuration file:

```text
android-app/app/google-services.json
```

3. Open `android-app/` in Android Studio.

4. Sync Gradle and run the `app` module on an emulator or physical device.

### Firebase Functions

1. Install dependencies:

```bash
cd functions
npm install
```

2. Build the functions:

```bash
npm run build
```

3. Select your Firebase project from the repository root:

```bash
npx -y firebase-tools@latest use --add <PROJECT_ID>
```

4. Deploy functions and Firestore rules:

```bash
npx -y firebase-tools@latest deploy --only functions,firestore:rules
```

## Configuration Notes

The current codebase includes project-specific endpoints and third-party integrations. Before using this app with a different Firebase project or production environment, review and update:

- Firebase Android configuration
- Firebase Functions base URL
- TMDB API configuration
- MoMo merchant/payment configuration
- Agora Voice SDK App ID
- EmailJS configuration
- Google Sign-In web client ID

### Utility Scripts Authentication

The Node scripts in `scripts/` use Google Application Default Credentials. Do not place Firebase service account JSON files in this repository.

For local development, authenticate before running scripts:

```bash
gcloud auth application-default login
export FIREBASE_PROJECT_ID=alpha-cinema-39dfb
cd scripts
npm start
```

If you must use a service account file locally, store it outside the repository and set `GOOGLE_APPLICATION_CREDENTIALS` to that absolute path.

## Security Notes

- Do not commit production secrets, service account files, private keys, or payment credentials.
- Move sensitive third-party credentials to secure runtime configuration before production deployment.
- Review Firestore rules and Cloud Function authorization before exposing the app publicly.

## License

This project is proprietary.

All rights reserved (c) Alpha Team.

---

<a id="tieng-viet"></a>

## Tiếng Việt

Alpha Cinema là ứng dụng xem phim trên Android được xây dựng bằng Kotlin và Jetpack Compose. Ứng dụng kết hợp khả năng khám phá phim, trình phát chất lượng cao, gợi ý phim bằng AI, Watch Party thời gian thực, thanh toán gói dịch vụ và các tính năng người dùng được hỗ trợ bởi Firebase.

## Điểm Nổi Bật

- Duyệt, tìm kiếm, lọc và xem phim với trải nghiệm tối ưu cho thiết bị di động.
- Khám phá phim thông qua chatbot AI hỗ trợ hỏi đáp và gợi ý bằng ngôn ngữ tự nhiên.
- Tạo và tham gia phòng Watch Party với đồng bộ phát phim, chat văn bản, giao tiếp giọng nói và hỗ trợ deep link.
- Quản lý tài khoản bằng Firebase Authentication, Google Sign-In, hồ sơ người dùng, danh sách yêu thích, lịch sử xem, đánh giá và bình luận.
- Hỗ trợ các gói đăng ký trả phí thông qua MoMo, với quy trình xác minh thanh toán bằng Firebase Cloud Functions.
- Gửi thông báo đẩy cho phim mới, gợi ý phim và nhắc nhở gói dịch vụ.
- Cung cấp công cụ admin để nhập phim, quản lý danh mục trang chủ, gán độ tuổi và hỗ trợ nội dung phù hợp cho trẻ em.

## Công Nghệ Sử Dụng

- **Mobile:** Kotlin, Jetpack Compose, Material 3, Navigation Compose, Coroutines, Flow
- **Media:** Media3/ExoPlayer, HLS, Coil, Lottie
- **Backend:** Firebase Auth, Firestore, Realtime Database, Storage, Cloud Messaging, Cloud Functions
- **Tích hợp:** MoMo SDK, Agora Voice SDK, Retrofit, OkHttp, TMDB API, EmailJS
- **Functions:** Node.js 20, TypeScript, Firebase Admin SDK

## Cấu Trúc Dự Án

```text
.
|-- android-app/        # Mã nguồn ứng dụng Android
|-- functions/          # Firebase Cloud Functions cho thanh toán, auth helpers và notifications
|-- scripts/            # Script tiện ích để nạp dữ liệu
|-- firestore.rules     # Quy tắc bảo mật Firestore
|-- firebase.json       # Cấu hình Firebase
`-- README.md
```

## Tính Năng Chính

### Xem Phim

- Màn hình trang chủ, tìm kiếm, danh mục, danh sách phim và chi tiết phim
- Chọn tập cho nội dung nhiều tập
- Phát HLS bằng Media3/ExoPlayer
- Lưu tiến độ xem và lịch sử xem phim

### Trợ Lý Phim AI

- Hỏi đáp và gợi ý phim bằng ngôn ngữ tự nhiên
- Gợi ý theo ngữ cảnh hội thoại tiếp diễn
- Cơ chế gợi ý dự phòng từ kho phim của ứng dụng
- Điều hướng trực tiếp trong app từ các gợi ý của chatbot

### Watch Party

- Tạo và tham gia phòng bằng mã phòng ngắn
- Đồng bộ trạng thái phát phim theo thời gian thực
- Danh sách thành viên và chat văn bản theo thời gian thực
- Giao tiếp giọng nói bằng Agora Voice SDK
- Hỗ trợ deep link `alphacinema://watchparty/{roomId}`

### Tài Khoản Và Tương Tác

- Đăng nhập bằng email/password và Google Sign-In
- Quản lý hồ sơ người dùng
- Yêu thích, đánh giá, bình luận và lịch sử xem
- Thông báo đẩy thông qua Firebase Cloud Messaging

### Thanh Toán Và Gói Dịch Vụ

- Luồng thanh toán MoMo và MoMo app
- Tạo và xác minh thanh toán ở phía server bằng Cloud Functions
- Đồng bộ trạng thái gói dịch vụ với Firestore

### Công Cụ Quản Trị

- Tìm kiếm và nhập phim từ API phim bên ngoài
- Lưu dữ liệu phim đã chuẩn hóa vào Firestore
- Quản lý danh mục hiển thị trên trang chủ
- Tự động đánh giá độ tuổi và trạng thái phù hợp với trẻ em

## Cài Đặt Và Chạy Dự Án

### Yêu Cầu

- Android Studio
- JDK 17 trở lên
- Node.js 20 cho Firebase Cloud Functions
- Một Firebase project đã cấu hình Android app
- Firebase CLI thông qua `npx -y firebase-tools@latest`

### Ứng Dụng Android

1. Clone repository:

```bash
git clone https://github.com/Huydinh2106/alpha-cinema.git
cd alpha-cinema
```

2. Thêm file cấu hình Firebase Android:

```text
android-app/app/google-services.json
```

3. Mở thư mục `android-app/` bằng Android Studio.

4. Sync Gradle và chạy module `app` trên emulator hoặc thiết bị thật.

### Firebase Functions

1. Cài dependencies:

```bash
cd functions
npm install
```

2. Build functions:

```bash
npm run build
```

3. Chọn Firebase project từ thư mục gốc của repository:

```bash
npx -y firebase-tools@latest use --add <PROJECT_ID>
```

4. Deploy functions và Firestore rules:

```bash
npx -y firebase-tools@latest deploy --only functions,firestore:rules
```

## Lưu Ý Cấu Hình

Codebase hiện tại có các endpoint và tích hợp bên thứ ba gắn với project cụ thể. Trước khi sử dụng với Firebase project khác hoặc môi trường production, cần kiểm tra và cập nhật:

- Cấu hình Firebase Android
- Firebase Functions base URL
- Cấu hình TMDB API
- Cấu hình merchant/payment của MoMo
- Agora Voice SDK App ID
- Cấu hình EmailJS
- Google Sign-In web client ID

## Lưu Ý Bảo Mật

- Không commit production secrets, service account files, private keys hoặc payment credentials.
- Nên đưa các thông tin nhạy cảm của bên thứ ba vào cấu hình runtime an toàn trước khi deploy production.
- Kiểm tra Firestore rules và quyền truy cập Cloud Functions trước khi public ứng dụng.

## Giấy Phép

Dự án này là phần mềm sở hữu riêng.

All rights reserved (c) Alpha Team.
