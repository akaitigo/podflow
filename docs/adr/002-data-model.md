# ADR-002: データモデル設計（Episode + Guest）

## ステータス

Accepted

## コンテキスト

Podflow は Podcast 制作ワークフローを管理するツールである。コアとなるデータモデル（Episode, Guest）の設計が必要。以下の判断を行う必要がある:

1. エンティティの構造と関連
2. 状態管理（EpisodeStatus）の設計
3. スキーマ管理方式（Flyway vs Hibernate auto-generation）
4. Guest の social_links の保存形式

## 決定

### エンティティ構造

- **Episode**: id (UUID), title, description, status, guest_id (FK), audio_url, show_notes, published_at, created_at, updated_at
- **Guest**: id (UUID), name, email, bio, social_links, created_at, updated_at
- 関連: Episode N:1 Guest（1エピソードに1ゲスト。将来の複数ゲスト対応は中間テーブルで拡張可能）

### EpisodeStatus（カンバンステージ）

`PLANNING -> GUEST_COORDINATION -> RECORDING -> EDITING -> REVIEW -> PUBLISHED`

- Kotlin 側は `enum class`、DB では `VARCHAR(50)` + `@Enumerated(EnumType.STRING)` で保存
- 整数値ではなく文字列で保存することで、DB の直接参照時の可読性と安全性を確保

### スキーマ管理: Flyway

- Hibernate `ddl-auto` ではなく Flyway を採用
- 理由: スキーマ変更の追跡可能性、本番環境での安全性、チーム開発での整合性
- 開発時: Flyway でマイグレーション実行、Hibernate は `validate` モード
- テスト時: H2 インメモリ + Hibernate `drop-and-create`（Flyway 無効化）

### social_links: TEXT カラム（JSON 文字列）

- PostgreSQL JSONB ではなく TEXT カラムに JSON 文字列を保存
- 理由: social_links はクエリ対象にならない表示用データであり、JSONB の利点（インデックス、パーシャルクエリ）が不要
- H2 テストとの互換性も確保

### Panache Repository パターン

- エンティティとリポジトリを分離する Repository パターンを採用
- 理由: テスタビリティの向上、関心の分離

## 結果

- Proto 定義が gRPC API の契約として機能する
- Kotlin エンティティは JPA 標準に準拠し、Panache Repository で CRUD 操作を提供
- Flyway マイグレーションでスキーマのバージョン管理が可能
- CI では H2 インメモリ DB によりテストが DB 非依存で実行可能
