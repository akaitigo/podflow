# Plan: Issue #1 プロジェクト基盤

## スコープ

CI/CD + lint + テストフレームワークを構築し、`make check` で全パスする状態にする。

## やらないこと

- Episode の gRPC サービス実装（Issue #2, #3）
- Playwright E2E テスト（Issue #5）
- gRPC-Web クライアント接続（Issue #4）
- Hooks 設定（CI が安定してから別途）

---

## タスク

### Phase 1: Proto 基盤
- [ ] proto/buf.yaml 作成（STANDARD + COMMENTS lint）
- [ ] proto/buf.gen.yaml 作成（Kotlin + TypeScript Connect 生成）
- [ ] buf lint / buf format 通過確認

### Phase 2: フロントエンド基盤
- [ ] frontend/biome.json 作成（Layer-1 テンプレートベース）
- [ ] frontend/vite.config.ts 作成（React プラグイン）
- [ ] frontend/package.json 更新（oxlint, coverage 追加）
- [ ] frontend/src/__tests__/App.test.tsx 作成（最小テスト1件）
- [ ] `cd frontend && pnpm install && pnpm lint && pnpm typecheck && pnpm test` 全パス

### Phase 3: バックエンド基盤
- [ ] Gradle wrapper 生成・コミット
- [ ] backend/build.gradle.kts 更新（detekt, kotlinter 追加）
- [ ] backend/detekt.yml 作成
- [ ] backend/src/main に最小クラス1つ（HealthCheck or Application）
- [ ] backend/src/test に最小テスト1つ
- [ ] `cd backend && ./gradlew build` 全パス

### Phase 4: 統合
- [ ] Makefile（ルート）作成: proto-lint, frontend-check, backend-check, check
- [ ] .github/workflows/ci.yml 作成（5ジョブ）
- [ ] `make check` ローカルで全パス

### Phase 5: 検証 + ドキュメント仕上げ
- [ ] PR 作成 → CI 全ジョブ green 確認
- [ ] CLAUDE.md のコマンド欄が実態と一致していることを確認
- [ ] README 更新: 実際に動くセットアップ手順（コピペで動く状態）
- [ ] docs/screens.md 更新: 実装後の画面スクリーンショット差し替え（ASCIIワイヤーフレーム→実画像）
- [ ] docs/use-cases.md ���新: 実装済み機能に合わせてフロー図を修正
- [ ] README に「構築済みの開発基盤」セクション追加（lint/test/CI の実行結果スクリーンショット）

---

## CI ジョブ構成

```yaml
jobs:
  proto-lint:     # buf lint + buf format --diff
  frontend:       # pnpm install → lint → typecheck → test
  backend:        # gradlew build（detekt + test 含む）
  build:          # needs: [proto-lint, frontend, backend]
                  # frontend build + backend build
  # e2e は Issue #5 で追加
```

---

## 判断が必要な点

1. **Connect vs gRPC-Web proxy**: buf.gen.yaml で Connect (ES) を使うが、Quarkus 側が Connect protocol をサポートするか未検証。Issue #2 で ADR を書いて判断する。MVP の Issue #1 では TypeScript コード生成の設定だけ入れ、実際の接続は Issue #3-4 で行う。

2. **Gradle wrapper のコミット**: gradle-wrapper.jar (約60KB) をリポジトリに含める（Gradle 公式推奨）。

3. **CI の PostgreSQL**: バックエンドテストで DB が必要になるのは Issue #3 以降。Issue #1 では DB 不要のテスト（HealthCheck 等）のみ。
