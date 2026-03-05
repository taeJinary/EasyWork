# 배포 도메인/쿠키/CORS 전략 고정안 (P0-1)

## 1) 목적
프론트엔드 연동 전 단계에서 인증 쿠키와 CORS 정책을 고정해, 운영 배포 시 계약 변경으로 인한 회귀를 방지한다.

## 2) 현재 백엔드 기준 고정 계약
현재 `develop` 기준 구현은 아래 정책을 전제로 한다.

- Refresh token 저장 위치: `HttpOnly Cookie only`
- Cookie 이름: `refresh_token`
- Cookie Path: `/api/v1/auth`
- Cookie SameSite: `Lax`
- Cookie Secure: `true` (`prod`)
- CORS: `allowCredentials=true`, `*` 금지, `https` Origin만 허용(`prod`)

근거 코드:
- `backend/src/main/java/com/taskflow/backend/domain/user/controller/AuthController.java`
- `backend/src/main/java/com/taskflow/backend/global/config/CookieSecurityPropertiesValidator.java`
- `backend/src/main/java/com/taskflow/backend/global/config/CorsOriginPropertiesValidator.java`
- `backend/src/main/resources/application-prod.yml`

## 3) 최종 결정 (현재 단계)
현재 단계의 기본 전략은 **same-site 운영**으로 고정한다.

의미:
- 프론트엔드와 백엔드는 동일 사이트(동일 eTLD+1) 기준으로 배포한다.
- 현재 `SameSite=Lax` 정책을 유지한다.
- 이 단계에서는 auth/cookie/CORS 계약을 변경하지 않는다.

권장 배포 예시:
- Frontend: `https://app.easywork.com`
- Backend: `https://api.easywork.com`

위 구성은 `same-site` 범주이므로 현재 쿠키 정책과 충돌하지 않는다.

## 4) prod 운영 정책
### 4-1. 필수 환경값
- `APP_CORS_ALLOWED_ORIGINS`는 실제 프론트 도메인만 명시한다.
- `localhost`, `127.0.0.1`, `*`는 금지한다.
- Origin은 `https://`만 허용한다.

예시:
```env
APP_CORS_ALLOWED_ORIGINS=https://app.easywork.com
```

### 4-2. refresh cookie 정책
아래 값은 운영 정책으로 고정한다.
```yaml
app:
  cookie:
    refresh-token-name: refresh_token
    refresh-token-path: /api/v1/auth
    secure: true
    same-site: Lax
```

## 5) cross-site 배포가 필요해질 경우 (현 단계 미적용)
프론트와 백엔드가 서로 다른 사이트로 분리될 경우(`same-site` 아님), 아래 변경이 필요하다.

1. Cookie 정책 변경
- `SameSite=None; Secure` 전환

2. CSRF 방어 전략 도입
- 예: Double Submit Cookie 또는 Origin/CSRF Token 검증

3. CORS 재정의
- `allowCredentials=true` 유지
- 명시적 Origin allowlist 유지(`*` 금지)

4. 검증기/테스트 업데이트
- `CookieSecurityPropertiesValidator` 정책 재정의
- auth/reissue/logout 통합 테스트를 cross-site 시나리오로 확장

cross-site 전환은 별도 변경 요청(PR)으로 진행하며, 이 문서를 함께 개정한다.

## 6) 배포 전 점검 체크리스트
- `application-prod.yml`와 실제 환경변수(`APP_CORS_ALLOWED_ORIGINS`) 일치 확인
- `Set-Cookie` 헤더 확인:
  - `HttpOnly`
  - `Secure`
  - `SameSite=Lax`
  - `Path=/api/v1/auth`
- 브라우저에서 `/auth/login -> /auth/token/reissue -> /auth/logout` 흐름 점검
- CORS preflight(`OPTIONS`) 및 credential 요청 점검

## 7) 계약 freeze 규칙 (이 단계)
아래 항목은 버그 수정이 아닌 한 변경하지 않는다.

- 인증 응답 구조
- refresh cookie 이름/경로/secure/same-site
- auth endpoint 경로
- WebSocket endpoint/prefix/destination 계약
- 에러코드 의미

필요 시 변경 사유, 프론트 영향 범위, 회귀 위험을 문서화한 뒤 승인 후 진행한다.

