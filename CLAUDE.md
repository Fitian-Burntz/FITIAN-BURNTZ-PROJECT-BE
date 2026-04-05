# FITIAN-BURNTZ PROJECT

CrossFit 박스 관리 서비스. 박스 운영자와 회원을 위한 앱 + 랜딩 웹 + 백엔드로 구성된 멀티레포 프로젝트.

## 전체 레포 구조

```
dev/
├── app/        # Flutter 앱 (iOS + Android) — 사용자/운영자용 모바일 앱
├── backend/    # Spring Boot (Java/Gradle) — REST API 서버
└── web/        # Next.js (TypeScript) — 랜딩 페이지
```

각 폴더는 독립적인 git 레포이며 GitHub에 별도로 관리됨.
- app: `main` 브랜치
- backend: `develop` 브랜치
- web: `main` 브랜치

---

## 이 레포 — backend (Spring Boot)

REST API 서버. Java/Gradle 기반.

`src/main/java/com/fitian/burntz/` 구조:
- `domain/` — 도메인별 패키지
  - `admin/` — 관리자
  - `alarm/` — 알림
  - `article/` — 게시글
  - `auth/` — 인증
  - `box/` — 박스
  - `channel/` — 채팅 채널
  - `classes/` — 수업
  - `member/` — 회원
  - `membership/` — 멤버십
  - `record/` — 기록
  - `wod/` — WOD
- `global/` — 공통 설정 (common, config, exception, security)
- `infra/` — 외부 연동 (claude, payment, s3)

주요 기술: Spring Boot, Java, Gradle, Docker, AWS ECS/ECR, GitHub Actions CI/CD
