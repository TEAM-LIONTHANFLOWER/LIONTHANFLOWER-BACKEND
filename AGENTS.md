# AGENTS.md

MCM Orbit 백엔드 레포지토리에서 Codex와 다른 코딩 에이전트가 지켜야 할 저장소 규칙입니다.

## Project Context
- Java 21, Spring Boot 4.1.0, Gradle, Spring Web MVC, JPA, Security, Flyway를 사용합니다.
- 기본 패키지는 `com.lionthanflower`입니다.
- 계층 구조가 필요한 경우 `domain`, `application`, `infrastructure`, `global` 구조를 우선 검토합니다.
- 데이터베이스와 배포 방식은 실제 설정 파일과 이슈를 먼저 확인한 뒤 변경합니다.

## Agent Instruction Policy
- `AGENTS.md`만 프로젝트 에이전트 규칙으로 사용합니다.
- 작업별 결정은 GitHub 이슈, PR 또는 `docs/tasks/{이슈 번호}/`에 둡니다.
- 사용자가 한국어로 작성하면 한국어로 응답하고, 한국어 문장은 `.`, `?`, `!`로 끝냅니다.

## Workflow
- 작업 전에 GitHub 이슈 번호를 확인합니다.
- 설계 판단이 필요하면 `docs/tasks/{이슈 번호}/design.md`, 여러 단계 계획이 필요하면 `docs/tasks/{이슈 번호}/plan.md`를 사용합니다.
- 새 작업에는 루트 `checklist.md`, `context-notes.md`, Spec Kit 파일을 만들지 않습니다.
- 실제 파일과 호출부를 확인하고 요청 범위만 변경합니다.

## Development Rules
- 설정은 `application-local.yml`, `application-dev.yml`, `application-prod.yml`의 역할을 확인한 뒤 환경별로 관리합니다.
- 공개 API 변경은 Springdoc annotation과 관련 테스트를 함께 갱신합니다.
- DB DDL 변경은 새 Flyway migration으로 남기고 적용된 migration은 수정하지 않습니다.
- Flyway migration 파일명은 현재 마지막 version 다음 번호를 사용합니다.
- 새 소스 파일 첫 줄에는 파일 역할을 설명하는 한 줄짜리 한국어 주석을 둡니다.

## Testing And Verification
- 코드 또는 설정 변경 후 `./gradlew test --stacktrace --no-daemon`을 실행합니다.
- 문서만 변경하면 `git diff --check`와 변경 범위를 검토합니다.
- 최종 응답에는 실제로 실행한 검사와 결과, 남은 위험을 포함합니다.

## Git
- 사용자 변경을 보존하고 dirty checkout에서는 관련 파일을 먼저 읽고 작업합니다.
- 커밋은 사용자가 요청했거나 작업 단위가 명확할 때만 수행합니다.
- 커밋 메시지는 `{이슈 번호} {type}: {한국어 메시지}` 형식을 사용합니다.