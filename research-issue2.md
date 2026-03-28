# Research: Issue #2 データモデル設計

## 1. EpisodeStatus の状態遷移

```
PLANNING → GUEST_COORDINATION → RECORDING → EDITING → REVIEW → PUBLISHED
```

- 基本は線形フロー（カンバンの左→右）
- 状態の巻き戻し（例: REVIEW → EDITING）は許容する（修正フロー）
- UNSPECIFIED (= 0) を proto3 のデフォルト値として含める（buf lint ENUM_ZERO_VALUE_SUFFIX 準拠）

## 2. Proto message/service 設計

### episode.proto
- `Episode` message: id (string/UUID), title, description, status, guest_id, audio_url, show_notes, published_at, created_at, updated_at
- `Guest` message: id, name, email, bio, social_links (repeated string)
- `EpisodeStatus` enum: EPISODE_STATUS_UNSPECIFIED = 0, PLANNING = 1, GUEST_COORDINATION = 2, RECORDING = 3, EDITING = 4, REVIEW = 5, PUBLISHED = 6

### episode_service.proto
- `EpisodeService`: CreateEpisode, GetEpisode, ListEpisodes, UpdateEpisode, DeleteEpisode
- Request/Response パターン: 各 RPC に専用の Request/Response message を定義
- ListEpisodes にはページネーション（page_size, page_token）を含める

### buf lint 準拠ポイント
- COMMENTS ルール: 全 message, enum, field, rpc にコメント必須
- ENUM_ZERO_VALUE_SUFFIX: enum の 0 値は `_UNSPECIFIED` サフィックス
- RPC Request/Response suffix: `XxxRequest` / `XxxResponse`

## 3. Kotlin エンティティ設計

### パターン選定: Panache Repository パターン
- Quarkus Hibernate ORM Panache Kotlin を使用
- Repository パターン（Entity と Repository を分離）を採用
  - テスタビリティが高い
  - エンティティがフレームワーク非依存に近い

### Episode エンティティ
```kotlin
@Entity
@Table(name = "episodes")
class Episode {
    @Id @GeneratedValue lateinit var id: UUID
    lateinit var title: String
    var description: String? = null
    @Enumerated(EnumType.STRING) var status: EpisodeStatus = EpisodeStatus.PLANNING
    @ManyToOne var guest: Guest? = null
    var audioUrl: String? = null
    @Column(columnDefinition = "TEXT") var showNotes: String? = null
    var publishedAt: Instant? = null
    lateinit var createdAt: Instant
    lateinit var updatedAt: Instant
}
```

### Guest エンティティ
```kotlin
@Entity
@Table(name = "guests")
class Guest {
    @Id @GeneratedValue lateinit var id: UUID
    lateinit var name: String
    var email: String? = null
    var bio: String? = null
    // social_links は JSON カラムまたは別テーブル
}
```

### social_links の保存方式
- 選択肢: (A) JSON カラム, (B) 正規化テーブル
- 決定: (A) JSON カラム — social_links は表示用途のみでクエリ対象にならないため
- PostgreSQL の JSONB 型 + JPA Converter を使用

## 4. Flyway vs Hibernate auto-generation

### 決定: Flyway
- 理由:
  1. スキーマ変更の履歴が明確（マイグレーションファイルで追跡可能）
  2. 本番環境で安全（Hibernate ddl-auto は本番では none/validate のみ推奨）
  3. チーム開発でのスキーマ管理が容易
- 開発時: Flyway でマイグレーション実行 + Hibernate validate
- CI: DB なしビルドではマイグレーション SQL の存在確認のみ

## 5. CI 考慮事項

### DB テスト問題
- CI 環境（GitHub Actions）では PostgreSQL サービスコンテナを使える
- 現時点では DB テストは不要（エンティティのコンパイル確認のみ）
- `@QuarkusTest` は DevServices で PostgreSQL を自動起動するが、CI で失敗する可能性あり
- 対策: テストプロファイル (`application.properties`) で DevServices を無効化し、H2 インメモリを使用
