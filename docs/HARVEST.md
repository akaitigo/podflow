# Harvest: podflow

> 日付: 2026-03-28
> パイプライン: Launch → Scaffold → Build → Harden → Ship → **Harvest**

## テンプレートの実戦評価

### 使えたもの
- [x] Makefile (proto-lint/frontend-check/backend-check/check) — 毎PRで使用
- [x] CI YAML (.github/workflows/ci.yml) — 全PRで自動実行、失敗0件
- [x] CLAUDE.md — ルール・コマンド定義が効果的
- [x] ADR テンプレート — 002 (Data Model) + 003 (gRPC-Web) を記録
- [x] biome.json (Layer-1 TypeScript) — lint/format 統合、テンプレートから適用
- [x] playwright.config.ts (Layer-2 Web App) — E2E 12シナリオ
- [x] buf.yaml / buf.gen.yaml (Layer-2 gRPC) — Proto パイプライン完全機能
- [x] startup.sh — 配置済み（ツール自動インストール機能あり）
- [x] 品質チェックリスト5項目 — 全達成

### 使えなかったもの
| ファイル | 理由 |
|---------|------|
| PostToolUse Hooks | guardian と同じ。hooks.json 未設定 |
| lefthook | lefthook 未インストール。guardian と同じ |
| startup.sh | 配置したが実行しなかった。guardian と同じ |
| session-end.sh | 同上 |
| CONTEXT.json | 生成せず。git log で代替 |
| progress.json | 使わなかった |
| quality-override.md | quality-checklist.md + CI で十分 |
| hooks-override.json | Hooks 自体が未設定のため不要 |

### Guardian との比較（2PJ共通の問題）

| 問題 | Guardian | Podflow | 結論 |
|------|----------|---------|------|
| Hooks 未設定 | ❌ | ❌ | **構造的問題**: 設定する動機と仕組みが弱い |
| startup.sh 未実行 | ❌ | ❌ | **同上**: idea-work スキルに組み込むべき |
| CONTEXT.json 不使用 | ❌ | ❌ | **不要**: git log で十分。テンプレートから削除検討 |
| テンプレート実装率 | ~70% | ~80% | Podflow が若干改善（E2E, ADR充実） |

### Podflow 固有の発見

| 発見 | 内容 | 対応 |
|------|------|------|
| **Kotlin テンプレートなし** | Layer-1 に Kotlin がない。独自に detekt.yml + build.gradle.kts を作成 | Kotlin テンプレート追加を検討 |
| **モノレポのCI設計** | 3ジョブ並列（proto/frontend/backend）が効果的 | 他のモノレポPJでも同パターン採用 |
| **H2 インメモリDB** | Testcontainers が WSL2 で動かない → H2 でテスト | CI/ローカル両対応の現実的な判断 |
| **dnd-kit とテスト** | D&D のクリックイベントがテスト/スクリーンショットと競合 | E2E テストでの回避策が必要 |

### テンプレートへの改善提案

| 提案 | 対象 | 内容 |
|------|------|------|
| Kotlin テンプレート追加 | layer-1-language | build.gradle.kts.template, detekt.yml, hooks.json, Makefile |
| CONTEXT.json 廃止検討 | layer-0-universal | 2PJ連続で不使用。git log で代替可能 |
| startup.sh 自動実行 | idea-work スキル | Step 3 冒頭で必ず実行する手順をハードコード |
| Hooks 設定の自動化 | idea-launch スキル | Step 5 でsettings.json への hooks 設定を自動注入 |
| 不要ファイル削減 | layer-0-universal | session-end.sh, progress.json, quality-override.md を整理 |

### 数値サマリ

| 指標 | 値 |
|------|-----|
| コミット数 | 8 (scaffold含む) |
| PR数 | 5 (全マージ、CI全グリーン) |
| ADR | 2件 |
| テスト (Frontend) | 24件 (Vitest) |
| テスト (Backend) | 45件 (JUnit 5) |
| テスト (E2E) | 12件 (Playwright) |
| CI失敗 | 0回 |
| テンプレート実装率 | 80% |
| 品質チェックリスト | 5/5 |

---

## レビューループ履歴 (2026-04-04)

> Stage: CI修正 + Review Loop (R1〜R5)

### Stage 0: CI修正 (PR 41/42)

| 問題 | 原因 | 修正 |
|------|------|------|
| TS2307 生成ファイル欠如 | buf generate がCI未実行、.gitignore で除外 | 生成済み .ts ファイルをコミット、.gitignore を修正 |
| GitHub workflow スコープ不足 | ci.yml を直接APIで書き込めない | contents APIでuserland修正に限定 |

### R1: Surface Review

| 深刻度 | 指摘 | 修正 |
|--------|------|------|
| MEDIUM | createEpisode/updateEpisode/deleteEpisode がエラー時に FETCH_ERROR を dispatch し loading: false に変更 | SET_ERROR アクションを追加 |
| MEDIUM | updateEpisode の existing.updatedAt = Instant.now() が冗長 (@PreUpdate と重複) | 削除 |
| LOW | timestampToIso の BigInt 精度制限が未文書 | JSDoc コメントを追記 |

### R2: Implementation Review (PR 43)

| 深刻度 | 指摘 | 修正 |
|--------|------|------|
| HIGH | audio_url に DB 制約 2048 文字があるがサービス層で未検証 | validateAudioUrl に長さチェックを追加 |
| MEDIUM | audio_url 長さチェックが detekt ThrowsCount 違反 | validateFieldLength 呼び出しに委譲 |
| LOW | .env.example が存在しない | 作成 |
| TEST | audio_url 2048文字超えテストなし | テスト追加 |

### R3: Design Review (PR 44)

| 深刻度 | 指摘 | 修正 |
|--------|------|------|
| CRITICAL | getEpisode/listEpisodes に @Transactional なし。guest lazy-load で LazyInitializationException | @Transactional 追加 |
| MEDIUM | parsePageToken が Int.MAX_VALUE を受け入れ、Panache page() でInt乗算オーバーフロー | MAX_PAGE_INDEX = 10000 で上限設定 |
| TEST | ゲスト付きエピソードの lazy-load テスト、pageToken 超過テストなし | テスト追加 |

### R4/R5: Integration + Robustness Review (PR 45)

| 深刻度 | 指摘 | 修正 |
|--------|------|------|
| MEDIUM | applyFieldUpdates の description が空文字保存、masked path は null 変換で不整合 | .ifEmpty { null } に統一 |
| MEDIUM | フロントエンドのフォーム入力に maxLength なし | CreateEpisodeModal/EpisodeDetailModal に maxLength 追加 |

### 残存 LOW 指摘 (対応不要と判断)

| 指摘 | 理由 |
|------|------|
| grpc-api.ts のゲスト名フォールバックが日本語ハードコード | 表示上の問題のみ |
| Offset ページネーションで並行挿入時に重複/欠落 | 既知の設計上の制約、MVPスコープ内 |
| Guest.socialLinks の JSON TEXT | GuestService 未実装のため到達不可能 |
| EpisodeMapper.toModelStatus が gRPC Status を知る | レイヤー懸念、実害なし |

### レビューループ数値サマリ

| 指標 | 値 |
|------|-----|
| レビューラウンド | 5 (R1〜R5) |
| 新規 PR | 3 (PR 43, 44, 45) |
| CI失敗→修正 | 2回 (ThrowsCount 違反 x2) |
| 修正ファイル数 | 7 |
| 発見・修正 | CRITICAL: 1, HIGH: 1, MEDIUM: 7, LOW: 3 (コメント対応) |
| EXIT条件達成 | CRITICAL=0, HIGH=0, MEDIUM=0 |

### テンプレートへの追加改善提案 (レビューループ後)

| 提案 | 内容 |
|------|------|
| detekt ThrowsCount パターンの周知 | throw が3個以上になりそうな関数は当初から委譲パターンを使う |
| @Transactional チェックリスト | gRPC Service メソッドは全て @Transactional を必須とするルールをCLAUDE.mdに追記 |
| フロントフォームの maxLength | バックエンドの文字数定数をフロントの maxLength と同期する仕組みを設計ルールに追加 |
