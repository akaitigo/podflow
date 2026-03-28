# podflow

Podcast制作のワークフローを一元管理するツール。企画→収録→編集→公開の各ステージをカンバンで管理し、ゲスト調整・ショーノート生成・配信プラットフォームへの一括公開を自動化。

## 技術スタック

- **フロントエンド**: TypeScript / React (Vite)
- **バックエンド**: Kotlin / Quarkus (gRPC)
- **通信**: gRPC + Protocol Buffers
- **データベース**: PostgreSQL
- **インフラ**: GCP Cloud Run, Cloud Storage

## セットアップ

### 前提条件

- Node.js 22+
- pnpm
- JDK 21+
- PostgreSQL 16+
- buf CLI

### フロントエンド

```bash
cd frontend
pnpm install
pnpm dev
```

### バックエンド

```bash
cd backend
./gradlew quarkusDev
```

### Proto

```bash
buf lint proto/
buf generate proto/
```

## ライセンス

MIT
