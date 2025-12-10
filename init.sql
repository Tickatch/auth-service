-- =============================================================================
-- Auth Service Database Initialization Script
-- =============================================================================

-- 스키마 생성
CREATE SCHEMA IF NOT EXISTS auth_service;

-- 스키마 설정
SET search_path TO auth_service;

-- UUID 확장 활성화
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- -----------------------------------------------------------------------------
-- Auth 테이블 (인증 정보)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS auths (
                                     id                      UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),

    -- 기본 정보
    email                   VARCHAR(255)    NOT NULL,
    user_type               VARCHAR(20)     NOT NULL,
    password                VARCHAR(500),

    -- 계정 상태
    status                  VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    login_fail_count        INTEGER         NOT NULL DEFAULT 0,
    last_login_at           TIMESTAMP,

    -- Audit 필드
    created_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by              VARCHAR(100),
    updated_at              TIMESTAMP,
    updated_by              VARCHAR(100),
    deleted_at              TIMESTAMP,
    deleted_by              VARCHAR(100),

    -- 제약 조건
    CONSTRAINT uk_auth_email_user_type UNIQUE (email, user_type),
    CONSTRAINT chk_user_type CHECK (user_type IN ('CUSTOMER', 'SELLER', 'ADMIN')),
    CONSTRAINT chk_auth_status CHECK (status IN ('ACTIVE', 'LOCKED', 'WITHDRAWN')),
    CONSTRAINT chk_login_fail_count CHECK (login_fail_count >= 0)
    );

-- Auth 인덱스
CREATE INDEX IF NOT EXISTS idx_auth_email ON auths(email);
CREATE INDEX IF NOT EXISTS idx_auth_user_type ON auths(user_type);
CREATE INDEX IF NOT EXISTS idx_auth_status ON auths(status);
CREATE INDEX IF NOT EXISTS idx_auth_deleted_at ON auths(deleted_at);

-- -----------------------------------------------------------------------------
-- AuthProvider 테이블 (소셜 로그인 연동 정보)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS auth_providers (
                                              id                      UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),

    -- 연동 정보
    auth_id                 UUID            NOT NULL,
    provider                VARCHAR(20)     NOT NULL,
    provider_user_id        VARCHAR(255)    NOT NULL,

    -- 연동 일시
    connected_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 제약 조건
    CONSTRAINT fk_auth_provider_auth FOREIGN KEY (auth_id) REFERENCES auths(id) ON DELETE CASCADE,
    CONSTRAINT uk_auth_provider UNIQUE (auth_id, provider),
    CONSTRAINT chk_provider_type CHECK (provider IN ('KAKAO', 'NAVER', 'GOOGLE'))
    );

-- AuthProvider 인덱스
CREATE INDEX IF NOT EXISTS idx_auth_provider_auth_id ON auth_providers(auth_id);
CREATE INDEX IF NOT EXISTS idx_auth_provider_provider ON auth_providers(provider);
CREATE INDEX IF NOT EXISTS idx_auth_provider_provider_user_id ON auth_providers(provider_user_id);

-- -----------------------------------------------------------------------------
-- RefreshToken 테이블 (리프레시 토큰)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS refresh_tokens (
                                              id                      UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),

    -- 토큰 정보
    auth_id                 UUID            NOT NULL,
    token                   VARCHAR(500)    NOT NULL,
    device_info             VARCHAR(500),

    -- 상태 정보
    expires_at              TIMESTAMP       NOT NULL,
    created_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked                 BOOLEAN         NOT NULL DEFAULT FALSE,
    remember_me             BOOLEAN         NOT NULL DEFAULT FALSE,

    -- 제약 조건
    CONSTRAINT uk_refresh_token UNIQUE (token)
    );

-- RefreshToken 인덱스
CREATE INDEX IF NOT EXISTS idx_refresh_token_auth_id ON refresh_tokens(auth_id);
CREATE INDEX IF NOT EXISTS idx_refresh_token_token ON refresh_tokens(token);
CREATE INDEX IF NOT EXISTS idx_refresh_token_expires_at ON refresh_tokens(expires_at);
CREATE INDEX IF NOT EXISTS idx_refresh_token_revoked ON refresh_tokens(revoked);

-- -----------------------------------------------------------------------------
-- 코멘트
-- -----------------------------------------------------------------------------
-- Auth
COMMENT ON TABLE auths IS '인증 정보 테이블';
COMMENT ON COLUMN auths.id IS '인증 ID (UUID)';
COMMENT ON COLUMN auths.email IS '이메일';
COMMENT ON COLUMN auths.user_type IS '사용자 유형 (CUSTOMER, SELLER, ADMIN)';
COMMENT ON COLUMN auths.password IS '암호화된 비밀번호';
COMMENT ON COLUMN auths.status IS '계정 상태 (ACTIVE, LOCKED, WITHDRAWN)';
COMMENT ON COLUMN auths.login_fail_count IS '로그인 실패 횟수 (5회 초과 시 LOCKED)';
COMMENT ON COLUMN auths.last_login_at IS '마지막 로그인 일시';
COMMENT ON COLUMN auths.created_at IS '생성 일시';
COMMENT ON COLUMN auths.created_by IS '생성자';
COMMENT ON COLUMN auths.updated_at IS '수정 일시';
COMMENT ON COLUMN auths.updated_by IS '수정자';
COMMENT ON COLUMN auths.deleted_at IS '삭제 일시';
COMMENT ON COLUMN auths.deleted_by IS '삭제자';

-- AuthProvider
COMMENT ON TABLE auth_providers IS '소셜 로그인 연동 정보 테이블';
COMMENT ON COLUMN auth_providers.id IS '소셜 연동 ID (UUID)';
COMMENT ON COLUMN auth_providers.auth_id IS '인증 ID (FK)';
COMMENT ON COLUMN auth_providers.provider IS '소셜 로그인 제공자 (KAKAO, NAVER, GOOGLE)';
COMMENT ON COLUMN auth_providers.provider_user_id IS '제공자 측 사용자 ID';
COMMENT ON COLUMN auth_providers.connected_at IS '연동 일시';

-- RefreshToken
COMMENT ON TABLE refresh_tokens IS '리프레시 토큰 테이블';
COMMENT ON COLUMN refresh_tokens.id IS '토큰 ID (UUID)';
COMMENT ON COLUMN refresh_tokens.auth_id IS '인증 ID';
COMMENT ON COLUMN refresh_tokens.token IS '토큰 값';
COMMENT ON COLUMN refresh_tokens.device_info IS '디바이스 정보';
COMMENT ON COLUMN refresh_tokens.expires_at IS '만료 일시';
COMMENT ON COLUMN refresh_tokens.created_at IS '생성 일시';
COMMENT ON COLUMN refresh_tokens.revoked IS '폐기 여부';
COMMENT ON COLUMN refresh_tokens.remember_me IS '로그인 유지 여부 (true: 30일, false: 1시간)';