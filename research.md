# Research: Issue #1 プロジェクト基盤

## 調査日: 2026-03-27

---

## 1. 3層テンプレートの適用方針

| レイヤー | テンプレート | 適用対象 |
|---------|------------|---------|
| Layer-0 | startup.sh, quality-checklist.md, Makefile.template | ルートレベル |
| Layer-1 (TypeScript) | biome.json, hooks.json, scripts/post-lint.sh | frontend/ |
| Layer-2 (Web App) | playwright.config.ts, hooks-override.json | frontend/ E2E |
| Layer-2 (gRPC Service) | buf.yaml, hooks-override.json | proto/, backend/ |

**注意**: Layer-1 に Kotlin テンプレートは存在しない。Kotlin 設定は手動構成が必要。

---

## 2. ツール選定

### フロントエンド (TypeScript/React)

| 用途 | ツール | 理由 |
|------|--------|------|
| Lint | Biome + oxlint | テンプレート準拠。Biome=フォーマット+lint統合、oxlint=高速ESLint互換 |
| テスト | Vitest | Vite ネイティブ。高速 |
| E2E | Playwright | Layer-2 web-app テンプレート準拠 |
| カバレッジ | @vitest/coverage-v8 | Vitest 標準プラグイン |
| gRPC-Web | @connectrpc/connect-web | buf 公式。Envoy proxy 不要 |

### バックエンド (Kotlin/Quarkus)

| 用途 | ツール | 理由 |
|------|--------|------|
| Lint | **detekt** | AST ベース静的解析。バグ+複雑度検出。Quarkus 実績あり |
| フォーマット | kotlinter (ktlint wrapper) | Gradle プラグインで簡単統合 |
| テスト | JUnit 5 + @QuarkusTest | Quarkus 標準 |
| DB テスト | Quarkus Dev Services | Testcontainers 自動起動（Docker 必須） |
| カバレッジ | JaCoCo (Quarkus 内蔵) | `quarkus-jacoco` 拡張 |

### Proto

| 用途 | ツール | 理由 |
|------|--------|------|
| Lint | buf lint (STANDARD + COMMENTS) | Layer-2 gRPC テンプレート準拠 |
| フォーマット | buf format | 標準 |
| 互換性 | buf breaking (FILE) | proto ルール準拠 |
| コード生成 | buf generate | Kotlin (protobuf-java) + TypeScript (connectrpc/es) |

---

## 3. CI/CD 設計

### guardian との比較

| 項目 | guardian (Go CLI) | podflow |
|------|------------------|---------|
| 言語数 | 1 | 2 (Kotlin + TypeScript) |
| CI ジョブ | 4 (lint, test, build, e2e) | 5 (proto, frontend, backend, build, e2e) |
| リンター | golangci-lint 1つ | detekt + kotlinter + biome + oxlint |
| E2E | bats (CLI) | Playwright (ブラウザ) |

### CI ジョブ構成案

```
proto-lint ──┐
             ├──→ build ──→ e2e
frontend ────┤
backend ─────┘
```

- **proto-lint**: buf lint + buf format --diff（高速、先行実行）
- **frontend**: pnpm install → biome + oxlint → typecheck → vitest
- **backend**: gradle build → detekt → test（Dev Services で PostgreSQL 自動起動）
- **build**: frontend build + backend build（needs: proto-lint, frontend, backend）
- **e2e**: Playwright（needs: build）— MVP では省略可、Issue #5 で追加

---

## 4. モノレポ構成の判断

**pnpm workspace は不要**。frontend/ のみが Node.js プロジェクト。ルートに package.json を置かず、frontend/ 内で完結させる。

ルートレベルの Makefile で統合:
```makefile
check: proto-lint frontend-check backend-check
```

---

## 5. 不足ファイル一覧

| ファイル | 状態 | 備考 |
|---------|------|------|
| frontend/biome.json | 未作成 | Layer-1 テンプレートからコピー+微調整 |
| frontend/vite.config.ts | 未作成 | React プラグイン設定 |
| backend/detekt.yml | 未作成 | 手動作成（テンプレートなし） |
| backend/Makefile | 未作成 | Gradle ラッパー |
| proto/buf.yaml | 未作成 | Layer-2 テンプレートからコピー |
| proto/buf.gen.yaml | 未作成 | Kotlin + TS Connect 生成設定 |
| .github/workflows/ci.yml | 未作成 | 5ジョブ構成 |
| Makefile (ルート) | 未作成 | 統合 check ターゲット |
| Gradle wrapper | 未作成 | `gradle wrapper` で生成 |

---

## 6. リスク

| リスク | 影響 | 対策 |
|--------|------|------|
| Gradle wrapper 未生成でCIビルド不可 | 高 | ローカルで `gradle wrapper` → コミット |
| buf generate の Kotlin 出力先が不正確 | 中 | buf.gen.yaml の out パスを検証 |
| Quarkus Dev Services がCI上で動かない | 中 | `quarkus.test.devservices.enabled=false` + services で PostgreSQL 起動 |
| Connect protocol の Quarkus 対応 | 中 | Quarkus gRPC は標準 gRPC。Connect は connect-kotlin か grpc-web proxy が必要 |
