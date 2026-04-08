# podflow

## 概要
Podcast制作のワークフローを一元管理するツール。企画→収録→編集→公開の各ステージをカンバンで管理し、ゲスト調整・ショーノート生成・配信プラットフォームへの一括公開を自動化。

## 技術スタック
- フロントエンド: TypeScript / React (Vite)
- バックエンド: Kotlin / Quarkus (gRPC)
- 通信: gRPC + Protocol Buffers
- データベース: PostgreSQL
- インフラ: GCP Cloud Run, Cloud Storage

## コーディングルール
- TypeScript: `~/.claude/rules/typescript.md` に従う（any禁止、as最小限）
- Kotlin: `~/.claude/rules/kotlin.md` に従う（!!禁止）
- Proto: `~/.claude/rules/proto.md` に従う（フィールド番号再利用禁止、buf lint/format）

## コマンド

### フロントエンド
- `cd frontend && pnpm dev` — 開発サーバー起動
- `cd frontend && pnpm build` — プロダクションビルド
- `cd frontend && pnpm lint` — Biome lint
- `cd frontend && pnpm typecheck` — 型チェック
- `cd frontend && pnpm test` — Vitest 実行

### バックエンド
- `cd backend && ./gradlew quarkusDev` — 開発モード起動
- `cd backend && ./gradlew build` — ビルド
- `cd backend && ./gradlew test` — テスト実行
- `cd backend && ./gradlew check` — lint + テスト

### Proto
- `buf lint proto/` — proto lint
- `buf format -w proto/` — proto フォーマット
- `buf generate proto/` — コード生成

## ディレクトリ構造
```
frontend/          — React SPA (Vite + TypeScript)
backend/           — Quarkus API (Kotlin + gRPC)
proto/             — Protocol Buffers 定義（共有）
docs/adr/          — Architecture Decision Records
.github/workflows/ — CI/CD
```

## ワークフロー
1. research.md を作成（調査結果の記録）
2. plan.md を作成（実装計画。人間承認まで実装禁止）
3. 承認後に実装開始

## ルール
- ADR: docs/adr/ 参照。新規決定はADRを書いてから実装
- テスト: 機能追加時は必ずテストを同時に書く
- lint設定の変更禁止（ADR必須）

## 禁止事項
- `any` 型 / `!!` 非null断言
- `console.log` / `println` のコミット
- TODO コメントのコミット（Issue化すること）
- .env・credentials のコミット

## 環境変数
```
DATABASE_URL=postgresql://localhost:5432/podflow
GCS_BUCKET=podflow-audio
DB_PASSWORD=<password>
JWT_EXPIRATION_MINUTES=60
CORS_ORIGINS=http://localhost:5173
```
