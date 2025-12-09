# Auth Service 설계 계획서

> Auth Service 구현을 위한 설계 방향과 구현 가이드입니다.

---

## 1. 서비스 개요

| 항목 | 내용 |
|------|------|
| **책임** | 인증(Authentication) - 회원가입, 로그인, 토큰 관리, 소셜 로그인 |
| **Aggregate Root** | Auth, RefreshToken |
| **주요 특징** | `email + userType` 복합 유니크 키 (동일 이메일로 구매자/판매자 분리 가입) |

---

## 2. 패키지 구조

```
src/main/java/com.tickatch.auth_service
├── auth/                               # Bounded Context
│   ├── presentation/
│   │   └── api/
│   │       ├── public/                 # 비인증 API (회원가입, 로그인)
│   │       │   ├── dto/
│   │       │   └── AuthPublicApi.java
│   │       └── internal/               # 내부 서비스 호출용
│   │           ├── dto/
│   │           └── AuthInternalApi.java
│   ├── application/
│   │   └── service/
│   │       ├── AuthService.java
│   │       ├── TokenService.java
│   │       └── OAuthService.java
│   ├── domain/
│   │   ├── Auth.java                   # Aggregate Root
│   │   ├── AuthProvider.java           # Entity (소셜 로그인)
│   │   ├── RefreshToken.java           # Aggregate Root
│   │   ├── vo/
│   │   │   ├── Password.java
│   │   │   ├── AuthStatus.java
│   │   │   ├── UserType.java
│   │   │   └── ProviderType.java
│   │   ├── service/                    # Domain Service (필요시)
│   │   ├── repository/
│   │   │   ├── AuthRepository.java
│   │   │   └── RefreshTokenRepository.java
│   │   └── exception/
│   │       ├── AuthErrorCode.java
│   │       └── AuthException.java
│   └── infrastructure/
│       └── external/
│           ├── jwt/
│           │   └── JwtTokenProvider.java
│           └── oauth/
│               ├── KakaoOAuthClient.java
│               ├── NaverOAuthClient.java
│               └── GoogleOAuthClient.java
│
└── global/
    ├── exception/
    ├── config/
    ├── utils/
    └── infrastructure/
        ├── event/
        │   └── dto/
        │       ├── AuthCreatedEvent.java
        │       └── AuthWithdrawnEvent.java
        └── domain/
            ├── AbstractTimeEntity.java
            └── AbstractAuditEntity.java
```

---

## 3. 도메인 설계

### 3.1 Auth (Aggregate Root)

**속성**

| 필드 | 타입 | 필수 | 설명 |
|------|------|:----:|------|
| `id` | `UUID` | O | PK |
| `email` | `String` | O | 이메일 |
| `userType` | `UserType` | O | CUSTOMER / SELLER / ADMIN |
| `password` | `Password` | O | VO (hash) |
| `status` | `AuthStatus` | O | ACTIVE / LOCKED / WITHDRAWN |
| `loginFailCount` | `int` | O | 로그인 실패 횟수 |
| `lastLoginAt` | `LocalDateTime` | X | 마지막 로그인 |
| `providers` | `List<AuthProvider>` | X | 소셜 로그인 목록 |

**제약조건**
```
UNIQUE(email, user_type)
```

**행위**
- `register()` - 회원가입
- `login()` / `recordLoginSuccess()` / `recordLoginFailure()` - 로그인
- `changePassword()` / `resetPassword()` - 비밀번호
- `lock()` / `unlock()` - 계정 잠금
- `withdraw()` - 탈퇴
- `connectProvider()` / `disconnectProvider()` - 소셜 로그인

### 3.2 AuthProvider (Entity)

| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | `UUID` | PK |
| `provider` | `ProviderType` | KAKAO / NAVER / GOOGLE |
| `providerUserId` | `String` | 소셜 서비스 사용자 ID |
| `connectedAt` | `LocalDateTime` | 연동 일시 |

### 3.3 RefreshToken (Aggregate Root)

| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | `UUID` | PK |
| `authId` | `UUID` | Auth 참조 |
| `token` | `String` | 토큰 값 |
| `deviceInfo` | `String` | 디바이스 정보 |
| `expiresAt` | `LocalDateTime` | 만료 일시 |

**행위**
- `create()` - 생성
- `rotate()` - 갱신 (Rotation)
- `isExpired()` - 만료 확인

### 3.4 Value Objects

| VO | 필드 | 검증 규칙 |
|----|------|----------|
| `Password` | hash | 8자 이상, 영문/숫자/특수문자 중 2가지 조합 |
| `AuthStatus` | enum | ACTIVE, LOCKED, WITHDRAWN |
| `UserType` | enum | CUSTOMER, SELLER, ADMIN |
| `ProviderType` | enum | KAKAO, NAVER, GOOGLE |

---

## 4. 예외 설계

### 4.1 ErrorCode 분류

| 분류 | HTTP Status | 예시 |
|------|-------------|------|
| 조회 실패 | 404 | AUTH_NOT_FOUND, REFRESH_TOKEN_NOT_FOUND |
| 검증 실패 | 400 | INVALID_EMAIL, INVALID_PASSWORD, DUPLICATE_EMAIL |
| 인증 실패 | 401 | INVALID_CREDENTIALS, TOKEN_EXPIRED |
| 권한 없음 | 403 | ACCOUNT_LOCKED, ACCOUNT_WITHDRAWN |
| 비즈니스 규칙 | 422 | LOGIN_FAILED_LIMIT_EXCEEDED |
| 외부 서비스 | 503 | OAUTH_SERVER_ERROR, EVENT_PUBLISH_FAILED |

### 4.2 AuthErrorCode 목록

```
# 조회 (404)
- AUTH_NOT_FOUND
- REFRESH_TOKEN_NOT_FOUND
- PROVIDER_NOT_FOUND

# 검증 - 회원가입 (400)
- INVALID_EMAIL
- INVALID_PASSWORD
- INVALID_USER_TYPE
- DUPLICATE_EMAIL

# 검증 - 비밀번호 (400)
- PASSWORD_MISMATCH
- SAME_AS_OLD_PASSWORD
- PASSWORD_TOO_SHORT
- PASSWORD_TOO_WEAK

# 검증 - 토큰 (400)
- INVALID_TOKEN
- EXPIRED_TOKEN
- INVALID_REFRESH_TOKEN
- EXPIRED_REFRESH_TOKEN

# 검증 - 소셜 (400)
- INVALID_PROVIDER
- INVALID_OAUTH_CODE
- PROVIDER_ALREADY_CONNECTED

# 인증 실패 (401)
- INVALID_CREDENTIALS
- TOKEN_EXPIRED
- AUTHENTICATION_FAILED

# 권한 없음 (403)
- ACCOUNT_LOCKED
- ACCOUNT_WITHDRAWN
- ACCESS_DENIED

# 비즈니스 규칙 (422)
- LOGIN_FAILED_LIMIT_EXCEEDED
- CANNOT_DISCONNECT_LAST_PROVIDER

# 외부 서비스 (503)
- OAUTH_SERVER_ERROR
- EVENT_PUBLISH_FAILED
```

### 4.3 메시지 파일 (messages_auth.properties)

```properties
# 조회 (404)
AUTH_NOT_FOUND=인증 정보를 찾을 수 없습니다.
REFRESH_TOKEN_NOT_FOUND=리프레시 토큰을 찾을 수 없습니다.
PROVIDER_NOT_FOUND=연동된 소셜 계정을 찾을 수 없습니다. (제공자: {0})

# 검증 - 회원가입 (400)
INVALID_EMAIL=이메일 형식이 올바르지 않습니다.
INVALID_PASSWORD=비밀번호는 8자 이상이어야 합니다.
INVALID_USER_TYPE=사용자 유형이 올바르지 않습니다.
DUPLICATE_EMAIL=이미 가입된 이메일입니다. (유형: {0})

# 검증 - 비밀번호 (400)
PASSWORD_MISMATCH=현재 비밀번호가 일치하지 않습니다.
SAME_AS_OLD_PASSWORD=새 비밀번호는 기존 비밀번호와 달라야 합니다.
PASSWORD_TOO_SHORT=비밀번호는 최소 8자 이상이어야 합니다.
PASSWORD_TOO_WEAK=비밀번호는 영문, 숫자, 특수문자 중 2가지 이상을 포함해야 합니다.

# 검증 - 토큰 (400)
INVALID_TOKEN=유효하지 않은 토큰입니다.
EXPIRED_TOKEN=만료된 토큰입니다.
INVALID_REFRESH_TOKEN=유효하지 않은 리프레시 토큰입니다.
EXPIRED_REFRESH_TOKEN=만료된 리프레시 토큰입니다.

# 검증 - 소셜 (400)
INVALID_PROVIDER=지원하지 않는 소셜 로그인 제공자입니다. (제공자: {0})
INVALID_OAUTH_CODE=유효하지 않은 인증 코드입니다.
PROVIDER_ALREADY_CONNECTED=이미 연동된 소셜 계정입니다. (제공자: {0})

# 인증 실패 (401)
INVALID_CREDENTIALS=이메일 또는 비밀번호가 일치하지 않습니다.
TOKEN_EXPIRED=인증이 만료되었습니다. 다시 로그인해주세요.
AUTHENTICATION_FAILED=인증에 실패했습니다.

# 권한 없음 (403)
ACCOUNT_LOCKED=계정이 잠금 상태입니다. 비밀번호 초기화를 진행해주세요.
ACCOUNT_WITHDRAWN=탈퇴한 계정입니다.
ACCESS_DENIED=접근 권한이 없습니다.

# 비즈니스 규칙 (422)
LOGIN_FAILED_LIMIT_EXCEEDED=로그인 실패 횟수 초과로 계정이 잠금되었습니다. (실패 횟수: {0})
CANNOT_DISCONNECT_LAST_PROVIDER=마지막 소셜 로그인 연동은 해제할 수 없습니다.

# 외부 서비스 (503)
OAUTH_SERVER_ERROR=소셜 로그인 서버에 문제가 발생했습니다. (제공자: {0})
EVENT_PUBLISH_FAILED=이벤트 발행에 실패했습니다. (인증 ID: {0})
```

---

## 5. 이벤트 설계

### 5.1 발행 이벤트

| 이벤트 | 발행 시점 | Routing Key | Payload |
|--------|----------|-------------|---------|
| `AuthCreatedEvent` | 회원가입 완료 | `auth.created` | authId, email, userType |
| `AuthWithdrawnEvent` | 회원 탈퇴 | `auth.withdrawn` | authId, userType |

### 5.2 구독 이벤트

| 이벤트 | 발행 서비스 | 처리 |
|--------|------------|------|
| `UserWithdrawnEvent` | User Service | Auth 상태 WITHDRAWN 변경 |

---

## 6. API 설계

### 6.1 Public API (비인증)

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/v1/auth/register` | 회원가입 |
| POST | `/api/v1/auth/login` | 로그인 |
| POST | `/api/v1/auth/refresh` | 토큰 갱신 |
| POST | `/api/v1/auth/password/reset` | 비밀번호 초기화 요청 |
| GET | `/api/v1/auth/oauth/{provider}` | 소셜 로그인 |
| GET | `/api/v1/auth/oauth/{provider}/callback` | 소셜 로그인 콜백 |

### 6.2 Authenticated API (인증 필요)

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/v1/auth/logout` | 로그아웃 |
| PUT | `/api/v1/auth/password` | 비밀번호 변경 |
| DELETE | `/api/v1/auth/withdraw` | 회원 탈퇴 |
| POST | `/api/v1/auth/providers/{provider}` | 소셜 계정 연동 |
| DELETE | `/api/v1/auth/providers/{provider}` | 소셜 계정 연동 해제 |

---

## 7. 비즈니스 규칙 요약

| 규칙 | 설명 |
|------|------|
| 복합 유니크 | `email + userType` 조합 유일 |
| 로그인 실패 | 5회 초과 시 자동 잠금 (LOCKED) |
| 비밀번호 정책 | 8자 이상, 영문/숫자/특수문자 중 2가지 조합 |
| 토큰 Rotation | Refresh Token 1회 사용 후 교체 |
| 토큰 만료 | Access 30분, Refresh 14일 |
| 소셜 연동 해제 | 비밀번호 없이 마지막 연동 해제 불가 |

---

## 8. 데이터베이스 스키마

```sql
-- auths 테이블
CREATE TABLE auths (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    user_type VARCHAR(20) NOT NULL,
    password_hash VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    login_fail_count INT NOT NULL DEFAULT 0,
    last_login_at TIMESTAMP,
    -- AbstractAuditEntity 필드
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(100),
    
    CONSTRAINT uk_auth_email_user_type UNIQUE (email, user_type)
);

-- auth_providers 테이블
CREATE TABLE auth_providers (
    id UUID PRIMARY KEY,
    auth_id UUID NOT NULL REFERENCES auths(id),
    provider VARCHAR(20) NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    connected_at TIMESTAMP NOT NULL,
    
    CONSTRAINT uk_auth_provider UNIQUE (auth_id, provider)
);

-- refresh_tokens 테이블
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    auth_id UUID NOT NULL,
    token VARCHAR(500) NOT NULL UNIQUE,
    device_info VARCHAR(255),
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL
);

-- 인덱스
CREATE INDEX idx_auths_email ON auths(email);
CREATE INDEX idx_auths_status ON auths(status);
CREATE INDEX idx_refresh_tokens_auth_id ON refresh_tokens(auth_id);
```

---

## 9. 구현 체크리스트

### Phase 1: 기본 구조
- [ ] 패키지 구조 생성
- [ ] global 공통 클래스 (AbstractTimeEntity, AbstractAuditEntity)
- [ ] AuthErrorCode, AuthException 구현
- [ ] messages_auth.properties 작성

### Phase 2: 도메인
- [ ] Auth Entity (Aggregate Root)
- [ ] AuthProvider Entity
- [ ] RefreshToken Entity
- [ ] Value Objects (Password, AuthStatus, UserType, ProviderType)
- [ ] Repository Interface

### Phase 3: 인프라
- [ ] JPA Repository 구현
- [ ] JwtTokenProvider 구현
- [ ] OAuth Client 구현 (Kakao, Naver, Google)

### Phase 4: 애플리케이션
- [ ] AuthService (회원가입, 로그인, 비밀번호)
- [ ] TokenService (토큰 발급, 갱신)
- [ ] OAuthService (소셜 로그인)

### Phase 5: 프레젠테이션
- [ ] DTO 정의
- [ ] AuthPublicApi (비인증 API)
- [ ] AuthInternalApi (내부 API)

### Phase 6: 이벤트
- [ ] AuthCreatedEvent 발행
- [ ] AuthWithdrawnEvent 발행
- [ ] UserWithdrawnEvent 구독