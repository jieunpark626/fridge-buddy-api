# 🥦 Fridge Buddy Server

보유 식재료 기반 보관 가이드 · 유통기한 관리 · 레시피 추천 서비스의 백엔드 서버입니다.

## Tech Stack

- **Language**: Kotlin 1.9 / Java 21
- **Framework**: Spring Boot 3.5
- **Database**: PostgreSQL 16 (JPA / Hibernate)
- **Build**: Gradle (Kotlin DSL)

## 로컬 실행

### 1. 환경변수 설정

```bash
cp .env.example .env.dev
```

`.env.dev` 파일을 열어 값을 채워주세요.

```env
DB_HOST=localhost
DB_PORT=5432
DB_NAME=your_db_name
DB_USERNAME=your_db_username
DB_PASSWORD=your_db_password
```

### 2. 실행 (Docker)

```bash
docker-compose --env-file .env.dev up -d
```

서버는 `http://localhost:8080` 에서 실행됩니다.
