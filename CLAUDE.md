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

---

## Obsidian Vault 자동 저장 규칙

이 프로젝트는 `vault/` 디렉토리를 Obsidian Second Brain으로 사용한다.  
**아래 작업을 수행할 때 자동으로 vault에 노트를 저장하거나 업데이트한다.**  
사용자에게 별도로 알릴 필요 없다 — 그냥 조용히 저장한다.

### 저장 트리거 & 경로

| 작업 | 저장 경로 | 규칙 |
|------|-----------|------|
| 특정 도메인 코드 분석 | `vault/Domains/{domain}.md` | 기존 파일이 있으면 관련 섹션만 업데이트 |
| 버그 발견 또는 수정 | `vault/Bugs/{YYYY-MM-DD}-{bug-name}.md` | 신규 생성 |
| 아키텍처·설계 결정 | `vault/Decisions/{YYYY-MM-DD}-{title}.md` | `vault/Templates/decision.md` 형식 사용 |
| 작업 세션 종료 (대화 마무리) | `vault/Daily/{YYYY-MM-DD}.md` | 해당 날짜 파일 없으면 생성, 있으면 append |
| API 스펙 변경 확인 | `vault/API/api-spec.md` | 변경된 섹션만 업데이트 |
| 코드 리뷰 수행 | `vault/CodeReview/{YYYY-MM-DD}-{scope}.md` | 신규 생성 |

### 날짜 형식

항상 `YYYY-MM-DD` 형식 사용. 오늘 날짜는 시스템에서 확인.

### 노트 작성 원칙

- 기존 파일이 있으면 **덮어쓰지 말고** 관련 섹션만 업데이트하거나 append
- frontmatter의 `updated` 필드를 오늘 날짜로 갱신
- 코드 위치 참조는 `파일경로:라인번호` 형식으로 기록
- 도메인 간 연결은 `[[다른노트]]` 형식으로 백링크 추가
