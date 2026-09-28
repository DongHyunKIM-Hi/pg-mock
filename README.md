# 가상 결제대행사 (pg-mock)

과제 1. 환불 요청 중계 서버의 외부 시스템 역할을 하는 서버. 계약은
`assignments/01-카카오뱅크/project/contract/pg-mock.yaml`(공개), `pg-mock-admin.yaml`(비공개, 평가 전용).

## 배포물

| 파일 | 용도 |
|---|---|
| `dist/pg-mock-arm64.tar` | 애플 실리콘 맥용 이미지 |
| `dist/pg-mock-amd64.tar` | 윈도우·인텔 맥·리눅스용 이미지 |
| `compose.yaml` | **수강생용.** admin API 비활성 |
| `compose.eval.yaml` | **튜터·평가용.** admin API 활성 (`X-Admin-Token` 없음, `docs/decisions.md` 2026-09-22 결정 참고) |

두 tar 모두 이미지 태그는 `refund-pg-mock:1.0.0`으로 동일하다. 자기 CPU에 맞는 파일 하나만 불러오면 된다.

## 실행 (수강생)

```
docker load -i pg-mock-arm64.tar      # 또는 pg-mock-amd64.tar
docker compose up
```

`http://localhost:9090/swagger-ui.html`에서 공개 API(`/pg/*`) 확인 가능. `/admin/*`은 404.

## 실행 (튜터, 검증용)

```
docker load -i pg-mock-arm64.tar
docker compose -f compose.eval.yaml up -d
curl -X POST http://localhost:9090/admin/reset
curl -X POST http://localhost:9090/admin/script -H 'Content-Type: application/json' \
  -d '{"refundId":"R-1","steps":["FAIL","SUCCESS"]}'
curl http://localhost:9090/admin/requests
curl http://localhost:9090/admin/processed
```

## 로컬 개발 (소스에서 바로 실행)

```
./gradlew bootRun                       # admin 비활성
ADMIN_ENABLED=true ./gradlew bootRun    # admin 활성 (포트 9090이 비어 있어야 함)
```

## 검증 완료 (2026-09-22)
- 컴파일·테스트 통과, Swagger에 `/pg/*`만 노출
- 실패 후 재시도 → 성공 → 재요청 시 `alreadyProcessed: true`
- **T1 함정**: 느린 처리 중 같은 ID로 재시도해도 이중 처리 안 됨 (같은 `processedAt` 반환)
- 같은 ID 동시 요청 20건 → 처리 기록 정확히 1건 (동시성 안전)
- admin 기본 비활성(404) / `ADMIN_ENABLED=true`일 때만 활성(204)
- `docker load` → `docker compose up` → API 호출까지 실제 컨테이너로 확인

## 남은 일
- [ ] Swagger 문서 예시·설명 다듬기
- [ ] Gradle wrapper 배포 시 프로젝트에 소스 포함 여부 결정 (수강생에게는 이미지만 주므로 소스는 강사용 저장소에만 유지)
