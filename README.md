# Auth Service

Tickatch 플랫폼의 **인증(Authentication) 마이크로서비스**입니다.

## 목차

- [개요](#개요)
- [아키텍처](#아키텍처)
- [도메인 모델](#도메인-모델)
- [API 명세](#api-명세)
- [이벤트 아키텍처](#이벤트-아키텍처)
- [JWT 토큰 관리](#jwt-토큰-관리)
- [OAuth 소셜 로그인](#oauth-소셜-로그인)
- [보안 정책](#보안-정책)
- [데이터베이스 스키마](#데이터베이스-스키마)
- [환경변수](#환경변수)
- [에러 코드](#에러-코드)

---

## 개요

Auth Service는 회원가입, 로그인, 토큰 관리, 소셜 로그인을 담당하며, 이벤트 기반 아키텍처를 통해 User Service와 상태를 동기화합니다.

### 핵심 특징

| 항목 | 설명 |
|------|------|
| **복합 유니크 키** | `email + userType` 조합 (동일 이메일로 구매자/판매자 분리 가입 가능) |
| **JWT RS256** | 비대칭키 기반 토큰 (Private Key 서명, Public Key 검증) |
| **토큰 Rotation** | Refresh Token 1회 사용 후 자동 교체 |
| **소셜 로그인** | Kakao, Naver, Google (CUSTOMER 전용) |

### 기술 스택

| 분류 | 기술 |
|------|------|
| Framework | Spring Boot 3.x |
| Language | Java 21 |
| Database | PostgreSQL 16 |
| Messaging | RabbitMQ (이벤트), Kafka (트레이싱) |
| Security | Spring Security, JWT (RS256), BCrypt |
| OAuth | OAuth 2.0 (Kakao, Naver, Google) |

---

## 아키텍처

### 시스템 구성

```
┌─────────────────────────────────────────────────────────────────┐
│                      Tickatch Platform                           │
├──────────────┬──────────────┬──────────────┬───────────────────┤
│     Auth     │     User     │   Product    │    Reservation    │
│   Service    │   Service    │   Service    │      Service      │
└──────┬───────┴──────┬───────┴──────────────┴───────────────────┘
       │              │
       │   RabbitMQ   │
       └──────┬───────┘
              │
        ┌─────┴─────┐
        │    Log    │
        │  Service  │
        └───────────┘
```

### 패키지 구조

```
src/main/java/com/tickatch/auth_service
├── auth/                               # Auth Bounded Context
│   ├── application/
│   │   ├── messaging/                  # 이벤트 발행 인터페이스
│   │   │   └── AuthLogEventPublisher.java
│   │   ├── port/out/                   # 아웃바운드 포트
│   │   │   ├── TokenPort.java          # Token 도메인 연동
│   │   │   └── OAuthPort.java          # OAuth 연동
│   │   └── service/
│   │       ├── command/                # 상태 변경 서비스
│   │       │   ├── AuthCommandService.java
│   │       │   ├── OAuthCommandService.java
│   │       │   └── dto/                # Command DTO
│   │       └── query/                  # 조회 서비스
│   │           ├── AuthQueryService.java
│   │           └── dto/                # Query DTO
│   ├── config/                         # Auth 전용 설정
│   ├── domain/
│   │   ├── Auth.java                   # Aggregate Root
│   │   ├── AuthProvider.java           # 소셜 연동 Entity
│   │   ├── AuthRepository.java
│   │   ├── exception/
│   │   │   ├── AuthException.java
│   │   │   └── AuthErrorCode.java
│   │   ├── repository/dto/             # Repository 조회 DTO
│   │   └── vo/
│   │       ├── AuthStatus.java         # ACTIVE, LOCKED, WITHDRAWN
│   │       ├── UserType.java           # CUSTOMER, SELLER, ADMIN
│   │       ├── ProviderType.java       # KAKAO, NAVER, GOOGLE
│   │       └── Password.java           # 비밀번호 VO
│   ├── infrastructure/
│   │   ├── adapter/                    # 포트 구현체
│   │   │   ├── TokenAdapter.java
│   │   │   └── OAuthAdapter.java
│   │   ├── messaging/
│   │   │   ├── config/                 # RabbitMQ 설정
│   │   │   │   └── RabbitMQConfig.java
│   │   │   ├── consumer/               # 이벤트 수신
│   │   │   │   └── UserEventConsumer.java
│   │   │   ├── event/                  # 이벤트 클래스
│   │   │   │   ├── AuthActionType.java
│   │   │   │   ├── AuthLogEvent.java
│   │   │   │   └── UserStatusChangedEvent.java
│   │   │   └── publisher/              # 이벤트 발행
│   │   │       └── RabbitAuthLogPublisher.java
│   │   └── oauth/
│   │       ├── client/                 # OAuth 클라이언트
│   │       │   ├── OAuthClient.java
│   │       │   ├── AbstractOAuthClient.java
│   │       │   ├── KakaoOAuthClient.java
│   │       │   ├── NaverOAuthClient.java
│   │       │   └── GoogleOAuthClient.java
│   │       └── dto/                    # OAuth DTO
│   │           ├── OAuthUserInfo.java
│   │           ├── OAuthTokenResponse.java
│   │           ├── OAuthState.java
│   │           └── OAuthProperties.java
│   └── presentation/
│       └── api/
│           ├── AuthApi.java            # 인증 API
│           ├── OAuthApi.java           # OAuth API
│           └── dto/
│               ├── request/            # 요청 DTO
│               └── response/           # 응답 DTO
│
├── token/                              # Token Bounded Context
│   ├── application/
│   │   ├── port/out/                   # 아웃바운드 포트
│   │   │   ├── TokenProvider.java
│   │   │   └── AuthPort.java
│   │   └── service/
│   │       ├── command/
│   │       │   ├── TokenCommandService.java
│   │       │   └── dto/
│   │       └── query/
│   │           └── dto/
│   ├── domain/
│   │   ├── RefreshToken.java           # Aggregate Root
│   │   ├── exception/
│   │   │   ├── TokenException.java
│   │   │   └── TokenErrorCode.java
│   │   └── repository/
│   │       ├── RefreshTokenRepository.java
│   │       └── dto/
│   ├── infrastructure/
│   │   └── adapter/
│   │       └── AuthAdapter.java
│   └── presentation/
│
└── global/                             # 글로벌 설정
    ├── config/
    │   ├── SecurityConfig.java
    │   ├── AsyncConfig.java
    │   ├── ActorExtractor.java
    │   ├── FeignConfig.java
    │   ├── KafkaConsumerConfig.java
    │   ├── KafkaProducerConfig.java
    │   └── QueryDslConfig.java
    ├── domain/
    │   ├── AbstractTimeEntity.java
    │   └── AbstractAuditEntity.java
    ├── feign/
    │   ├── FeignErrorDecoder.java
    │   └── FeignRequestInterceptor.java
    └── jwt/
        ├── infrastructure/
        │   ├── JwtTokenProvider.java
        │   ├── JwtProperties.java
        │   └── RsaKeyManager.java
        └── presentation/
            └── JwtKeyController.java   # JWKS 엔드포인트
```

---

## 도메인 모델

### Auth (Aggregate Root)

인증 정보를 관리하는 핵심 엔티티입니다.

```java
@Entity
@Table(name = "auths",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_auth_email_user_type",
        columnNames = {"email", "user_type"}))
public class Auth extends AbstractAuditEntity {
    private UUID id;
    private String email;
    private UserType userType;
    private Password password;          // Embedded VO
    private AuthStatus status;
    private int loginFailCount;
    private LocalDateTime lastLoginAt;
    private List<AuthProvider> providers;
}
```

**행위 메서드**

| 메서드 | 설명 |
|--------|------|
| `register()` | 일반 회원가입 |
| `registerWithOAuth()` | OAuth 회원가입 (CUSTOMER 전용) |
| `recordLoginSuccess()` | 로그인 성공 기록 |
| `recordLoginFailure()` | 로그인 실패 기록 (5회 초과 시 잠금) |
| `changePassword()` | 비밀번호 변경 |
| `resetPassword()` | 비밀번호 초기화 (잠금 해제) |
| `lock()` / `unlock()` | 계정 잠금/해제 |
| `withdraw()` | 회원 탈퇴 |
| `connectProvider()` | 소셜 계정 연동 |
| `disconnectProvider()` | 소셜 계정 연동 해제 |

### AuthProvider (Entity)

소셜 로그인 연동 정보를 저장합니다.

```java
@Entity
@Table(name = "auth_providers")
public class AuthProvider {
    private UUID id;
    private Auth auth;
    private ProviderType provider;      // KAKAO, NAVER, GOOGLE
    private String providerUserId;
    private LocalDateTime connectedAt;
}
```

### RefreshToken (Aggregate Root)

Refresh Token을 관리하며, Rotation 방식을 지원합니다.

```java
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {
    private UUID id;
    private UUID authId;
    private String token;
    private String deviceInfo;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private boolean revoked;
    private boolean rememberMe;
}
```

**토큰 만료 정책**

| 조건 | 만료 시간 |
|------|----------|
| 로그인 유지 미선택 | 1시간 |
| 로그인 유지 선택 | 30일 |

### Value Objects

#### AuthStatus

```
ACTIVE ←─→ LOCKED ──→ ACTIVE (비밀번호 초기화)
   │
   ↓
WITHDRAWN (최종 상태)
```

| 상태 | 설명 |
|------|------|
| `ACTIVE` | 활성 상태 |
| `LOCKED` | 잠금 상태 (로그인 실패 5회 초과) |
| `WITHDRAWN` | 탈퇴 상태 (최종) |

#### UserType

| 유형 | 설명 | OAuth 허용 |
|------|------|:----------:|
| `CUSTOMER` | 일반 구매자 | ✅ |
| `SELLER` | 판매자 | ❌ |
| `ADMIN` | 관리자 | ❌ |

#### ProviderType

| 제공자 | 설명 |
|--------|------|
| `KAKAO` | 카카오 |
| `NAVER` | 네이버 |
| `GOOGLE` | 구글 |

---

## API 명세

### 인증 API

#### 비인증 API (Public)

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/v1/auth/register` | 회원가입 |
| POST | `/api/v1/auth/login` | 로그인 |
| POST | `/api/v1/auth/refresh` | 토큰 갱신 |
| POST | `/api/v1/auth/check-email` | 이메일 중복 확인 |
| POST | `/api/v1/auth/password/reset` | 비밀번호 초기화 요청 |

#### 인증 필요 API

| Method | Endpoint | 설명 | 헤더 |
|--------|----------|------|------|
| GET | `/api/v1/auth/me` | 내 정보 조회 | X-User-Id |
| POST | `/api/v1/auth/logout` | 로그아웃 | X-User-Id |
| PUT | `/api/v1/auth/password` | 비밀번호 변경 | X-User-Id |
| DELETE | `/api/v1/auth/withdraw` | 회원탈퇴 | X-User-Id |

### OAuth API

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|:----:|
| GET | `/api/v1/auth/oauth/{provider}` | OAuth 로그인 (리다이렉트) | X |
| GET | `/api/v1/auth/oauth/{provider}/callback` | OAuth 콜백 | X |
| GET | `/api/v1/auth/oauth/{provider}/link` | 소셜 계정 연동 | O |
| DELETE | `/api/v1/auth/oauth/{provider}/unlink` | 소셜 계정 연동 해제 | O |

### JWKS API

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/.well-known/jwks.json` | JWKS (JSON Web Key Set) |
| GET | `/.well-known/public-key.pem` | Public Key (PEM 형식) |

### 요청/응답 예시

#### 회원가입

**Request**
```json
POST /api/v1/auth/register
{
  "email": "user@example.com",
  "password": "Password1!",
  "userType": "CUSTOMER",
  "rememberMe": false
}
```

**Response**
```json
{
  "success": true,
  "message": "회원가입이 완료되었습니다.",
  "data": {
    "authId": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@example.com",
    "userType": "CUSTOMER",
    "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "a1b2c3d4e5f6...",
    "accessTokenExpiresAt": "2025-01-01T00:05:00",
    "refreshTokenExpiresAt": "2025-01-01T01:00:00"
  }
}
```

#### 로그인

**Request**
```json
POST /api/v1/auth/login
{
  "email": "user@example.com",
  "password": "Password1!",
  "userType": "CUSTOMER",
  "rememberMe": true
}
```

#### 토큰 갱신

**Request**
```json
POST /api/v1/auth/refresh
{
  "refreshToken": "a1b2c3d4e5f6..."
}
```

---

## 이벤트 아키텍처

### 이벤트 흐름

```
┌─────────────────────────────────────────────────────────────────┐
│                         RabbitMQ                                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌─────────────────┐         ┌─────────────────┐                │
│  │  tickatch.user  │         │  tickatch.log   │                │
│  │   (Exchange)    │         │   (Exchange)    │                │
│  └────────┬────────┘         └────────┬────────┘                │
│           │                           │                          │
│  User Service                   Auth Service                     │
│  (발행)                         (발행)                           │
│           │                           │                          │
│           ↓                           ↓                          │
│  ┌─────────────────┐         ┌─────────────────┐                │
│  │  Auth Service   │         │   Log Service   │                │
│  │    (수신)       │         │    (수신)       │                │
│  └─────────────────┘         └─────────────────┘                │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 수신 이벤트 (User Service → Auth Service)

| Queue | Routing Keys | 처리 |
|-------|--------------|------|
| `tickatch.user.withdrawn.auth.queue` | `customer.withdrawn`, `seller.withdrawn`, `admin.withdrawn` | Auth 상태 WITHDRAWN, 토큰 삭제 |
| `tickatch.user.suspended.auth.queue` | `customer.suspended`, `seller.suspended`, `admin.suspended` | Auth 상태 LOCKED, 토큰 무효화 |
| `tickatch.user.activated.auth.queue` | `customer.activated`, `seller.activated`, `admin.activated` | Auth 상태 ACTIVE |

### 발행 이벤트 (Auth Service → Log Service)

| Exchange | Routing Key | Queue | 설명 |
|----------|-------------|-------|------|
| `tickatch.log` | `auth.log` | `tickatch.auth.log.queue` | 인증 로그 |

### AuthLogEvent Payload

```java
public record AuthLogEvent(
    UUID eventId,
    UUID authId,
    String userType,
    String actionType,
    String actorType,
    UUID actorUserId,
    LocalDateTime occurredAt
) {}
```

### Action Types

| 카테고리 | Action Type | 설명 |
|----------|-------------|------|
| 회원가입 | `REGISTERED` | 회원가입 성공 |
| | `REGISTER_FAILED` | 회원가입 실패 |
| | `OAUTH_REGISTERED` | OAuth 회원가입 성공 |
| | `OAUTH_REGISTER_FAILED` | OAuth 회원가입 실패 |
| 로그인 | `LOGIN` | 로그인 성공 |
| | `LOGIN_FAILED` | 로그인 실패 |
| | `OAUTH_LOGIN` | OAuth 로그인 성공 |
| | `OAUTH_LOGIN_FAILED` | OAuth 로그인 실패 |
| | `LOGOUT` | 로그아웃 |
| | `LOGOUT_FAILED` | 로그아웃 실패 |
| 토큰 | `TOKEN_REFRESHED` | 토큰 갱신 성공 |
| | `TOKEN_REFRESH_FAILED` | 토큰 갱신 실패 |
| 비밀번호 | `PASSWORD_CHANGED` | 비밀번호 변경 성공 |
| | `PASSWORD_CHANGE_FAILED` | 비밀번호 변경 실패 |
| 탈퇴 | `WITHDRAWN` | 탈퇴 성공 |
| | `WITHDRAW_FAILED` | 탈퇴 실패 |
| 상태 동기화 | `USER_WITHDRAWN_SYNCED` | 탈퇴 이벤트 동기화 |
| | `USER_SUSPENDED_SYNCED` | 정지 이벤트 동기화 |
| | `USER_ACTIVATED_SYNCED` | 활성화 이벤트 동기화 |
| OAuth 연동 | `PROVIDER_LINKED` | 소셜 계정 연동 |
| | `PROVIDER_LINK_FAILED` | 소셜 계정 연동 실패 |
| | `PROVIDER_UNLINKED` | 소셜 계정 연동 해제 |
| | `PROVIDER_UNLINK_FAILED` | 소셜 계정 연동 해제 실패 |

### RabbitMQ 설정

```java
// Exchanges
tickatch.user (Topic)       // User Service 이벤트 수신
tickatch.log (Topic)        // Log Service로 발행
tickatch.user.dlx (DLX)     // Dead Letter Exchange
tickatch.log.dlx (DLX)      // Dead Letter Exchange

// Queues with DLQ
tickatch.user.withdrawn.auth.queue  → tickatch.user.withdrawn.auth.queue.dlq
tickatch.user.suspended.auth.queue  → tickatch.user.suspended.auth.queue.dlq
tickatch.user.activated.auth.queue  → tickatch.user.activated.auth.queue.dlq
tickatch.auth.log.queue             → tickatch.auth.log.queue.dlq
```

---

## JWT 토큰 관리

### RS256 비대칭키 구조

```
┌─────────────────────────────────────────────────────────────────┐
│                    Auth Service                                  │
│  ┌─────────────────┐                                            │
│  │   Private Key   │ ──── 서명 ────→ Access Token               │
│  │   (보관)        │                                            │
│  └─────────────────┘                                            │
│  ┌─────────────────┐                                            │
│  │   Public Key    │ ──── JWKS 제공 ────→ Gateway / Services    │
│  │   (공개)        │                                            │
│  └─────────────────┘                                            │
└─────────────────────────────────────────────────────────────────┘
```

### 키 로딩 우선순위

1. **환경변수**: `JWT_PRIVATE_KEY`, `JWT_PUBLIC_KEY` (Base64 인코딩)
2. **지정 경로**: `jwt.private-key-path`, `jwt.public-key-path`
3. **기본 디렉토리**: `data/keys/private.pem`, `data/keys/public.pem`
4. **자동 생성**: 키가 없으면 2048bit RSA 키 쌍 생성

### Access Token 구조

```json
{
  "header": {
    "alg": "RS256",
    "typ": "JWT",
    "kid": "tickatch-auth-key-1"
  },
  "payload": {
    "sub": "550e8400-e29b-41d4-a716-446655440000",
    "userType": "CUSTOMER",
    "iss": "tickatch",
    "iat": 1704067200,
    "exp": 1704067500
  }
}
```

### 토큰 정책

| 토큰 | 만료 시간 | 비고 |
|------|----------|------|
| Access Token | 5분 | 탈취 시 피해 최소화 |
| Refresh Token (기본) | 1시간 | 로그인 유지 미선택 |
| Refresh Token (유지) | 30일 | 로그인 유지 선택 |

### Rotation 정책

1. Refresh Token 사용 시 새 토큰으로 교체
2. 이미 사용된 토큰 재사용 감지 시 모든 토큰 폐기 (보안)

---

## OAuth 소셜 로그인

### 지원 제공자

| 제공자 | Authorization URI | Token URI | User Info URI |
|--------|-------------------|-----------|---------------|
| Kakao | `https://kauth.kakao.com/oauth/authorize` | `https://kauth.kakao.com/oauth/token` | `https://kapi.kakao.com/v2/user/me` |
| Naver | `https://nid.naver.com/oauth2.0/authorize` | `https://nid.naver.com/oauth2.0/token` | `https://openapi.naver.com/v1/nid/me` |
| Google | `https://accounts.google.com/o/oauth2/v2/auth` | `https://oauth2.googleapis.com/token` | `https://www.googleapis.com/oauth2/v2/userinfo` |

### OAuth 흐름

```
1. 프론트엔드 → GET /api/v1/auth/oauth/{provider}?rememberMe=true
                     │
2.                   ↓ (리다이렉트)
   사용자 ←──── 소셜 로그인 페이지
                     │
3.                   ↓ (로그인 완료)
   Auth Service ←── GET /api/v1/auth/oauth/{provider}/callback?code=...&state=...
                     │
4.                   ↓ (code로 액세스 토큰 요청)
   OAuth Provider ─── Access Token
                     │
5.                   ↓ (사용자 정보 요청)
   OAuth Provider ─── User Info (email, name, profile)
                     │
6.                   ↓ (회원가입 또는 로그인)
   프론트엔드 ←──── 토큰 응답 (리다이렉트)
```

### OAuthState (state 파라미터)

```java
public record OAuthState(
    String nonce,           // CSRF 방지용 난수
    boolean rememberMe,     // 로그인 유지 여부
    String deviceInfo,      // 디바이스 정보
    UUID linkAuthId         // 계정 연동 시 기존 Auth ID
) {}
```

---

## 보안 정책

### 비밀번호 정책

| 항목 | 규칙 |
|------|------|
| 최소 길이 | 8자 이상 |
| 문자 조합 | 영문, 숫자, 특수문자 중 **2가지 이상** |
| 허용 특수문자 | `!@#$%^*()_+-=.,?` |
| 암호화 | BCrypt |

### 계정 잠금 정책

| 항목 | 규칙 |
|------|------|
| 잠금 조건 | 로그인 실패 **5회** 초과 |
| 해제 방법 | 비밀번호 초기화 또는 관리자 해제 |
| 상태 | `LOCKED` |

### 소셜 로그인 정책

| 사용자 유형 | 소셜 로그인 | 사유 |
|------------|:-----------:|------|
| CUSTOMER | ✅ 허용 | 일반 고객, 편의성 중요 |
| SELLER | ❌ 불가 | 사업자 인증 필요, 보안 강화 |
| ADMIN | ❌ 불가 | 내부 시스템, 최고 보안 필요 |

### Security 설정

```java
// Permit All 경로
POST /api/v1/auth/register
POST /api/v1/auth/login
POST /api/v1/auth/refresh
POST /api/v1/auth/check-email
POST /api/v1/auth/password/reset
/api/v1/auth/oauth/**
/.well-known/**
```

---

## 데이터베이스 스키마

### auths 테이블

```sql
CREATE TABLE auths (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    user_type VARCHAR(20) NOT NULL,
    password VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    login_fail_count INT NOT NULL DEFAULT 0,
    last_login_at TIMESTAMP,
    
    -- Audit 필드
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(100),
    
    CONSTRAINT uk_auth_email_user_type UNIQUE (email, user_type)
);

CREATE INDEX idx_auths_email ON auths(email);
CREATE INDEX idx_auths_status ON auths(status);
```

### auth_providers 테이블

```sql
CREATE TABLE auth_providers (
    id UUID PRIMARY KEY,
    auth_id UUID NOT NULL REFERENCES auths(id),
    provider VARCHAR(20) NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    connected_at TIMESTAMP NOT NULL,
    
    CONSTRAINT uk_auth_provider UNIQUE (auth_id, provider)
);

CREATE INDEX idx_auth_providers_provider_user_id 
    ON auth_providers(provider, provider_user_id);
```

### refresh_tokens 테이블

```sql
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    auth_id UUID NOT NULL,
    token VARCHAR(500) NOT NULL UNIQUE,
    device_info VARCHAR(255),
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT false,
    remember_me BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_refresh_tokens_auth_id ON refresh_tokens(auth_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
```

---

## 환경변수

### .env.example

```env
# ========================================
# Tickatch Auth Service 환경 변수
# ========================================

# ===== 애플리케이션 기본 설정 =====
APP_NAME=auth-service
APP_PROFILE=default
APP_VERSION=1.0.0
SERVER_PORT=8090

# ===== 환경 식별 =====
ENVIRONMENT=production

# ===== Eureka 설정 =====
# Eureka 서버 URL (HA 구성)
EUREKA_DEFAULT_ZONE=https://your-domain.com/eureka1/eureka/,https://your-domain.com/eureka2/eureka/
# 이 서비스 인스턴스의 호스트명
EUREKA_INSTANCE_HOSTNAME=your-instance-ip

# ===== Config Server 설정 =====
CONFIG_SERVER_URL=https://your-domain.com/config

# ===== Kafka 설정 =====
KAFKA_BOOTSTRAP_SERVERS=your-kafka-host:9094

# ===== RabbitMQ 설정 =====
RABBITMQ_HOST=your-rabbitmq-host
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=your-username
RABBITMQ_PASSWORD=your-password
RABBITMQ_VHOST=/

# ===== 데이터베이스 설정 =====
DB_HOST=your-db-host
DB_PORT=5432
DB_NAME=tickatch
DB_USERNAME=your-db-username
DB_PASSWORD=your-db-password

# ===== 로깅 설정 =====
LOG_LEVEL=INFO
HIBERNATE_LOG_LEVEL=WARN

# ===== JPA 설정 =====
JPA_DDL_AUTO=validate
JPA_SHOW_SQL=false

# ===== 트레이싱 설정 =====
TRACING_PROBABILITY=0.1
ZIPKIN_ENDPOINT=https://your-domain.com/zipkin/api/v2/spans

# ===== JWT 설정 =====
# Secret Key (최소 256bit = 32자 이상, Base64 인코딩 권장)
# 생성 명령어: openssl rand -base64 64
JWT_SECRET=your-256-bit-secret-key-here
JWT_ISSUER=tickatch
JWT_ACCESS_EXPIRATION=3000

# ===== OAuth2 공통 =====
OAUTH_BASE_REDIRECT_URI=http://localhost:8090

# ===== Kakao OAuth =====
# https://developers.kakao.com/console/app 에서 발급
OAUTH_KAKAO_CLIENT_ID=your-kakao-client-id
OAUTH_KAKAO_CLIENT_SECRET=your-kakao-client-secret

# ===== Naver OAuth =====
# https://developers.naver.com/apps 에서 발급
OAUTH_NAVER_CLIENT_ID=your-naver-client-id
OAUTH_NAVER_CLIENT_SECRET=your-naver-client-secret

# ===== Google OAuth =====
# https://console.cloud.google.com/apis/credentials 에서 발급
OAUTH_GOOGLE_CLIENT_ID=your-google-client-id
OAUTH_GOOGLE_CLIENT_SECRET=your-google-client-secret
```

### 환경변수 설명

| 변수 | 설명 | 예시 |
|------|------|------|
| `APP_NAME` | 애플리케이션 이름 | `auth-service` |
| `SERVER_PORT` | 서버 포트 | `8090` |
| `JWT_SECRET` | JWT 서명 키 (32자 이상) | Base64 인코딩된 키 |
| `JWT_ISSUER` | JWT 발급자 | `tickatch` |
| `JWT_ACCESS_EXPIRATION` | Access Token 만료 시간 (초) | `3000` |
| `OAUTH_BASE_REDIRECT_URI` | OAuth Redirect URI 기본 도메인 | `http://localhost:8090` |
| `OAUTH_KAKAO_CLIENT_ID` | 카카오 클라이언트 ID | developers.kakao.com에서 발급 |
| `OAUTH_NAVER_CLIENT_ID` | 네이버 클라이언트 ID | developers.naver.com에서 발급 |
| `OAUTH_GOOGLE_CLIENT_ID` | 구글 클라이언트 ID | console.cloud.google.com에서 발급 |

---

## 에러 코드

### AuthErrorCode

| 분류 | 코드 | HTTP | 메시지 |
|------|------|:----:|--------|
| **조회** | `AUTH_NOT_FOUND` | 404 | 인증 정보를 찾을 수 없습니다. |
| | `PROVIDER_NOT_FOUND` | 404 | 연동된 소셜 계정을 찾을 수 없습니다. |
| | `SOCIAL_ACCOUNT_NOT_FOUND` | 404 | 소셜 계정을 찾을 수 없습니다. |
| **회원가입** | `INVALID_EMAIL` | 400 | 이메일 형식이 올바르지 않습니다. |
| | `INVALID_PASSWORD` | 400 | 비밀번호에 허용되지 않은 문자가 포함되어 있습니다. |
| | `INVALID_USER_TYPE` | 400 | 사용자 유형이 올바르지 않습니다. |
| | `DUPLICATE_EMAIL` | 400 | 이미 가입된 이메일입니다. |
| | `EMAIL_ALREADY_EXISTS` | 400 | 이미 존재하는 이메일입니다. |
| **비밀번호** | `PASSWORD_MISMATCH` | 400 | 현재 비밀번호가 일치하지 않습니다. |
| | `SAME_AS_OLD_PASSWORD` | 400 | 새 비밀번호는 기존 비밀번호와 달라야 합니다. |
| | `PASSWORD_TOO_SHORT` | 400 | 비밀번호는 최소 8자 이상이어야 합니다. |
| | `PASSWORD_TOO_WEAK` | 400 | 비밀번호는 영문, 숫자, 특수문자 중 2가지 이상을 포함해야 합니다. |
| | `INVALID_CURRENT_PASSWORD` | 400 | 현재 비밀번호가 일치하지 않습니다. |
| **OAuth** | `INVALID_PROVIDER` | 400 | 지원하지 않는 소셜 로그인 제공자입니다. |
| | `INVALID_OAUTH_CODE` | 400 | 유효하지 않은 인증 코드입니다. |
| | `INVALID_OAUTH_STATE` | 400 | 유효하지 않은 상태 값입니다. |
| | `PROVIDER_ALREADY_CONNECTED` | 400 | 이미 연동된 소셜 계정입니다. |
| | `OAUTH_NOT_ALLOWED_FOR_USER_TYPE` | 400 | 소셜 로그인은 일반 고객만 사용 가능합니다. |
| | `OAUTH_PROVIDER_NOT_CONFIGURED` | 400 | OAuth 제공자가 설정되지 않았습니다. |
| | `OAUTH_LOGIN_CANCELLED` | 400 | 로그인이 취소되었습니다. |
| | `OAUTH_EMAIL_REQUIRED` | 400 | 이메일 정보가 필요합니다. |
| | `USER_TYPE_MISMATCH` | 400 | 사용자 유형이 일치하지 않습니다. |
| **인증 실패** | `INVALID_CREDENTIALS` | 401 | 이메일 또는 비밀번호가 일치하지 않습니다. |
| | `AUTHENTICATION_FAILED` | 401 | 인증에 실패했습니다. |
| **권한 없음** | `ACCOUNT_LOCKED` | 403 | 계정이 잠금 상태입니다. |
| | `ACCOUNT_WITHDRAWN` | 403 | 탈퇴한 계정입니다. |
| | `ACCESS_DENIED` | 403 | 접근 권한이 없습니다. |
| **비즈니스** | `LOGIN_FAILED_LIMIT_EXCEEDED` | 422 | 로그인 실패 횟수 초과로 계정이 잠금되었습니다. |
| | `ALREADY_WITHDRAWN` | 422 | 이미 탈퇴한 계정입니다. |
| **외부 서비스** | `OAUTH_SERVER_ERROR` | 503 | 소셜 로그인 서버에 문제가 발생했습니다. |
| | `OAUTH_TOKEN_FAILED` | 503 | OAuth 토큰 발급에 실패했습니다. |
| | `OAUTH_USER_INFO_FAILED` | 503 | OAuth 사용자 정보 조회에 실패했습니다. |
| | `EVENT_PUBLISH_FAILED` | 503 | 이벤트 발행에 실패했습니다. |

### TokenErrorCode

| 분류 | 코드 | HTTP | 메시지 |
|------|------|:----:|--------|
| **조회** | `REFRESH_TOKEN_NOT_FOUND` | 404 | 리프레시 토큰을 찾을 수 없습니다. |
| | `AUTH_NOT_FOUND` | 404 | 인증 정보를 찾을 수 없습니다. |
| **검증** | `INVALID_TOKEN` | 400 | 유효하지 않은 토큰입니다. |
| | `INVALID_REFRESH_TOKEN` | 400 | 유효하지 않은 리프레시 토큰입니다. |
| | `INVALID_TOKEN_FORMAT` | 400 | 토큰 형식이 올바르지 않습니다. |
| **만료** | `TOKEN_EXPIRED` | 401 | 토큰이 만료되었습니다. |
| | `REFRESH_TOKEN_EXPIRED` | 401 | 리프레시 토큰이 만료되었습니다. |
| **비즈니스** | `TOKEN_ALREADY_REVOKED` | 422 | 이미 폐기된 토큰입니다. |
| | `TOKEN_REUSE_DETECTED` | 422 | 토큰 재사용이 감지되었습니다. |

---

## 의존성 (common-lib)

| 모듈 | 설명 |
|------|------|
| `ApiResponse` / `PageResponse` | 표준 응답 포맷 |
| `BusinessException` / `ErrorCode` | 에러 처리 |
| `BaseSecurityConfig` / `LoginFilter` | 보안 설정 |
| `DomainEvent` / `IntegrationEvent` | 이벤트 래핑 |
| `JsonUtils` | JSON 직렬화 |
| `MdcUtils` | 분산 추적 |

---

## 관련 서비스

| 서비스 | 통신 방식 | 설명 |
|--------|----------|------|
| User Service | RabbitMQ | 사용자 상태 이벤트 수신 |
| Log Service | RabbitMQ | 인증 로그 발행 |
| Gateway | JWKS | Public Key 제공 (토큰 검증) |

---

© 2025 Tickatch Team