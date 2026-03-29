# ADR-004: テスト環境で H2 インメモリ DB を使用する

## ステータス

Accepted

## コンテキスト

バックエンドのテストで PostgreSQL 互換のデータベースを使用するか、H2 インメモリ DB を使用するかの判断が必要。

### 検討した選択肢

1. **Testcontainers + PostgreSQL**: 本番同等の DB でテスト。高い信頼性だが、Docker 必須。WSL2 環境で Docker-in-Docker の安定性に課題あり。
2. **H2 インメモリ DB**: 軽量で高速。Docker 不要。PostgreSQL 固有機能（JSONB 等）は検証できない。
3. **組み込み PostgreSQL**: embedded-postgres ライブラリを使用。セットアップが複雑。

## 決定

**H2 インメモリ DB をテスト環境で使用する。**

理由:

1. **開発環境の制約**: 主要開発環境が WSL2 であり、Testcontainers（Docker-in-Docker）の動作が不安定。
2. **MVP スコープ**: 現時点で PostgreSQL 固有の機能（JSONB、全文検索等）を使用しておらず、H2 で十分にカバーできる。
3. **CI 速度**: H2 はコンテナ起動が不要なため、CI パイプラインが高速。
4. **Hibernate ORM の抽象化**: Panache + Hibernate ORM 経由でアクセスしており、SQL 方言の差異は最小限。

## リスク

- PostgreSQL 固有の型や関数を使い始めた場合、H2 テストでは検出できない差異が生じる。
  - **軽減策**: PostgreSQL 固有機能を導入する際に Testcontainers への移行を検討する。
  - **軽減策**: Flyway マイグレーションの互換性を意識した SQL を書く。

## 結果

- テスト環境では `quarkus-jdbc-h2` + `quarkus-test-h2` を使用
- `application.properties` のテストプロファイルで H2 を設定
- PostgreSQL 固有機能の導入時に本 ADR を再評価
