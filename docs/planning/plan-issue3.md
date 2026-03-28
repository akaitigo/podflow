# Plan: Issue #3 — Episode CRUD gRPC サービス実装

## 前提
- Issue #2 で proto 定義、エンティティ、リポジトリ、Flyway マイグレーションは作成済み
- Quarkus gRPC (Mutiny API) で実装
- テストは H2 インメモリDB + @QuarkusTest

## 実装ステップ

### Step 1: proto ファイルを Quarkus が認識する場所に配置
- `backend/src/main/proto/` にシンボリックリンクまたは直接ファイルを配置
- Quarkus は `src/main/proto` 内の `.proto` を自動検出してスタブ生成
- `build.gradle.kts` に proto 依存（`com.google.protobuf:protobuf-java`）が必要な場合追加

### Step 2: ステータス遷移ロジック追加
- ファイル: `EpisodeStatus.kt`
- `allowedTransitions` マップと `canTransitionTo()` メソッドを追加
- 純粋ロジックなので Quarkus 依存なし

### Step 3: エンティティ ↔ proto 変換ヘルパー
- ファイル: `service/EpisodeMapper.kt`（新規）
- `Episode` エンティティ → proto `Episode` メッセージ変換
- proto `EpisodeStatus` ↔ Kotlin `EpisodeStatus` 変換
- `Instant` ↔ `Timestamp` 変換

### Step 4: gRPC サービス実装
- ファイル: `service/EpisodeGrpcService.kt`（新規）
- `@GrpcService` + Mutiny API ベース
- 5 メソッド: CreateEpisode, GetEpisode, ListEpisodes, UpdateEpisode, DeleteEpisode
- `@Transactional` でトランザクション管理
- gRPC `StatusException` でエラーハンドリング

### Step 5: テスト実装
- `EpisodeStatusTest.kt` — ステータス遷移ユニットテスト
- `EpisodeGrpcServiceTest.kt` — gRPC CRUD 統合テスト（@QuarkusTest）

### Step 6: ビルド検証
- `./gradlew build` 全パス（lint + compile + test）

## ファイル構成（新規・変更）

```
backend/
  src/main/proto/
    podflow/v1/episode.proto          (コピー)
    podflow/v1/episode_service.proto  (コピー)
  src/main/kotlin/com/akaitigo/podflow/
    model/EpisodeStatus.kt            (変更: 遷移ロジック追加)
    service/EpisodeMapper.kt          (新規)
    service/EpisodeGrpcService.kt     (新規)
  src/test/kotlin/com/akaitigo/podflow/
    model/EpisodeStatusTest.kt        (新規)
    service/EpisodeGrpcServiceTest.kt (新規)
```

## リスク
- Quarkus の gRPC コード生成が proto の `java_package` 設定を正しく反映するか確認が必要
- H2 の UUID 型対応（H2 は UUID をネイティブサポートするので問題ないはず）
- Mutiny の `Uni` 内での `@Transactional` 動作（Quarkus が自動対応）
