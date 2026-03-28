# Plan: Issue #2 データモデル設計

## タスク分解

### Phase 1: Proto 定義
- [ ] `proto/podflow/v1/episode.proto` — Episode, Guest, EpisodeStatus を定義
- [ ] `proto/podflow/v1/episode_service.proto` — CRUD RPC を定義
- [ ] `buf lint proto/` 通過確認
- [ ] `buf format -w proto/` 実行

### Phase 2: Backend 依存関係
- [ ] `backend/build.gradle.kts` — PostgreSQL + Hibernate ORM Panache + Flyway 依存を追加
- [ ] `backend/src/main/resources/application.properties` — DB 設定追加

### Phase 3: Kotlin エンティティ
- [ ] `Episode.kt` — Episode エンティティ + EpisodeStatus enum
- [ ] `Guest.kt` — Guest エンティティ
- [ ] `EpisodeRepository.kt` — Panache Repository
- [ ] `GuestRepository.kt` — Panache Repository

### Phase 4: Flyway マイグレーション
- [ ] `V1__create_episodes_and_guests.sql` — テーブル作成 SQL

### Phase 5: テスト設定
- [ ] テスト用 `application.properties` — DevServices 無効化 + H2 インメモリ
- [ ] `build.gradle.kts` — H2 テスト依存追加
- [ ] ビルド確認 (`./gradlew build -x test` or `./gradlew build`)

### Phase 6: ADR
- [ ] `docs/adr/002-data-model.md` — データモデル設計の判断記録

### Phase 7: 検証 & PR
- [ ] `make proto-lint` 通過
- [ ] `./gradlew build` 通過（テスト含む）
- [ ] PR 作成 → CI 確認 → マージ

## ファイル一覧（新規/変更）

| ファイル | 操作 |
|---|---|
| `proto/podflow/v1/episode.proto` | 変更 |
| `proto/podflow/v1/episode_service.proto` | 新規 |
| `backend/build.gradle.kts` | 変更 |
| `backend/src/main/resources/application.properties` | 変更 |
| `backend/src/test/resources/application.properties` | 新規 |
| `backend/src/main/kotlin/com/akaitigo/podflow/model/Episode.kt` | 新規 |
| `backend/src/main/kotlin/com/akaitigo/podflow/model/EpisodeStatus.kt` | 新規 |
| `backend/src/main/kotlin/com/akaitigo/podflow/model/Guest.kt` | 新規 |
| `backend/src/main/kotlin/com/akaitigo/podflow/repository/EpisodeRepository.kt` | 新規 |
| `backend/src/main/kotlin/com/akaitigo/podflow/repository/GuestRepository.kt` | 新規 |
| `backend/src/main/resources/db/migration/V1__create_episodes_and_guests.sql` | 新規 |
| `docs/adr/002-data-model.md` | 新規 |
