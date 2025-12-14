# Tickatch Auth Service

티켓 예매 플랫폼 **Tickatch**의 인증(Authentication) 마이크로서비스입니다.

## 프로젝트 소개

Tickatch는 콘서트, 뮤지컬, 연극, 스포츠 등 다양한 공연의 티켓 예매를 지원하는 플랫폼입니다. Auth Service는 회원가입, 로그인, 토큰 관리, 소셜 로그인을 담당하며, 이벤트 기반 아키텍처를 통해 User Service와 통신합니다.

> 🚧 **MVP 단계** - 현재 핵심 기능 개발 중입니다.

## 기술 스택

| 분류 | 기술 |
|------|------|
| Framework | Spring Boot 3.x |
| Language | Java 21+ |
| Database | PostgreSQL |
| Messaging | RabbitMQ |
| Security | Spring Security, JWT |
| OAuth | OAuth 2.0 (Kakao, Naver, Google) |

## 아키텍처

### 시스템 구성

```
┌─────────────────────────────────────────────────────────────┐
│                        Tickatch Platform                     │
├─────────────┬─────────────┬─────────────┬───────────────────┤
│    Auth     │    User     │   Product   │    Reservation    │
│   Service   │   Service   │   Service   │      Service      │
└──────┬──────┴──────┬──────┴─────────────┴───────────────────┘
       │             │
       └──────┬──────┘
              │
         RabbitMQ
```

### 레이어 구조

```
src/main/java
├── auth/                           # Bounded Context
│   ├── presentation/
│   │   └── api/
│   │       ├── public/             # 비인증 API
│   │       │   ├── dto/
│   │       │   └── AuthPublicApi
│   │       └── internal/           # 내부 서비스 호출용
│   │           ├── dto/
│   │           └── AuthInternalApi
│   ├── application/
│   │   └── service/
│   │       ├── AuthService
│   │       └── TokenService
│   ├── domain/
│   │   ├── Auth                    # Aggregate Root (Entity)
│   │   ├── AuthProvider            # Entity
│   │   ├── RefreshToken            # Aggregate Root (Entity)
│   │   ├── vo/
│   │   │   ├── Password
│   │   │   ├── AuthStatus
│   │   │   ├── UserType
│   │   │   └── ProviderType
│   │   ├── service/                # Domain Service
│   │   ├── repository/
│   │   │   ├── AuthRepository
│   │   │   └── RefreshTokenRepository
│   │   └── exception/
│   │       ├── AuthException
│   │       └── AuthErrorCode
│   └── infrastructure/
│       └── external/               # 외부 API Client
│           ├── jwt/
│           │   └── JwtTokenProvider
│           └── oauth/
│               ├── KakaoOAuthClient
│               ├── NaverOAuthClient
│               └── GoogleOAuthClient
│
└── global/
    ├── exception/
    │   ├── GlobalExceptionHandler
    │   └── ErrorResponse
    ├── config/
    │   ├── SecurityConfig
    │   └── RabbitMQConfig
    ├── utils/
    └── infrastructure/
        ├── event/
        │   └── dto/
        │       ├── AuthCreatedEvent
        │       └── AuthWithdrawnEvent
        └── domain/
            ├── AbstractTimeEntity
            └── AbstractAuditEntity
```

## 주요 기능

### 인증 관리
- 회원가입 (이메일 + 비밀번호)
- 로그인 / 로그아웃
- 비밀번호 변경 / 초기화
- 회원 탈퇴

### 토큰 관리
- JWT Access Token 발급
- Refresh Token 관리 (Rotation)
- 토큰 갱신 / 폐기

### 소셜 로그인
- 카카오 로그인
- 네이버 로그인
- 구글 로그인

### 사용자 타입

| 타입 | 설명 |
|------|------|
| `CUSTOMER` | 일반 구매자 |
| `SELLER` | 판매자 (공연 등록) |
| `ADMIN` | 관리자 |

### 인증 상태 흐름

```
(가입) ──→ ACTIVE ──→ LOCKED ──→ ACTIVE
              │                     │
              └─────────────────────┴──→ WITHDRAWN
```

| 상태 | 설명 |
|------|------|
| ACTIVE | 활성 (정상) |
| LOCKED | 잠금 (로그인 실패 5회 초과) |
| WITHDRAWN | 탈퇴 |

### 제약조건

```
UNIQUE(email, user_type)
```
- 동일 이메일로 CUSTOMER, SELLER 각각 가입 가능

## API 명세

### 인증 API

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| POST | `/api/v1/auth/register` | 회원가입 | X |
| POST | `/api/v1/auth/login` | 로그인 | X |
| POST | `/api/v1/auth/logout` | 로그아웃 | O |
| POST | `/api/v1/auth/refresh` | 토큰 갱신 | X |
| PUT | `/api/v1/auth/password` | 비밀번호 변경 | O |
| POST | `/api/v1/auth/password/reset` | 비밀번호 초기화 요청 | X |
| DELETE | `/api/v1/auth/withdraw` | 회원 탈퇴 | O |

### 소셜 로그인 API

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| GET | `/api/v1/auth/oauth/{provider}` | 소셜 로그인 | X |
| GET | `/api/v1/auth/oauth/{provider}/callback` | 소셜 로그인 콜백 | X |
| POST | `/api/v1/auth/providers/{provider}` | 소셜 계정 연동 | O |
| DELETE | `/api/v1/auth/providers/{provider}` | 소셜 계정 연동 해제 | O |

## 이벤트

### 발행 이벤트

회원가입/탈퇴 시 RabbitMQ를 통해 User Service로 이벤트를 발행합니다.

| 이벤트 | Routing Key | 대상 서비스 | Payload |
|--------|-------------|-------------|---------|
| AuthCreatedEvent | `auth.created` | User Service | authId, email, userType |
| AuthWithdrawnEvent | `auth.withdrawn` | User Service | authId, userType |

### 구독 이벤트

| 이벤트 | Routing Key | 발행 서비스 | 처리 |
|--------|-------------|-------------|------|
| UserWithdrawnEvent | `user.withdrawn` | User Service | Auth 상태 WITHDRAWN 변경 |

## 보안 정책

### 비밀번호 정책
- 최소 8자 이상
- 영문, 숫자, 특수문자 중 2가지 이상 조합
- BCrypt 암호화 저장

### 로그인 실패 정책
- 5회 실패 시 계정 잠금 (LOCKED)
- 비밀번호 초기화 또는 관리자 해제로 잠금 해제

### 토큰 정책
- Access Token: 30분
- Refresh Token: 14일
- Refresh Token Rotation 적용

## 실행 방법

### 환경 변수

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/tickatch_auth
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  rabbitmq:
    host: localhost
    port: 5672
    username: ${RABBITMQ_USERNAME}
    password: ${RABBITMQ_PASSWORD}

jwt:
  secret: ${JWT_SECRET}
  access-token-expiry: 1800000      # 30분
  refresh-token-expiry: 1209600000  # 14일

oauth:
  kakao:
    client-id: ${KAKAO_CLIENT_ID}
    client-secret: ${KAKAO_CLIENT_SECRET}
  naver:
    client-id: ${NAVER_CLIENT_ID}
    client-secret: ${NAVER_CLIENT_SECRET}
  google:
    client-id: ${GOOGLE_CLIENT_ID}
    client-secret: ${GOOGLE_CLIENT_SECRET}
```

### 실행

```bash
./gradlew bootRun
```

## 관련 서비스

- **User Service** - 사용자 프로필 관리
- **Product Service** - 상품(공연) 관리
- **Reservation Service** - 예매 관리

---

© 2025 Tickatch Team