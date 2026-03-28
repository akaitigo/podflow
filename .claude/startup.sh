#!/usr/bin/env bash
# =============================================================================
# セッション起動ルーチン（原則6: セッション間状態管理）
#
# セッション開始時に実行し、前セッションの状態を復元する。
# CONTEXT.json（目標）+ progress.json（履歴）の両方を読み込む。
#
# オプション:
#   --dev    開発サーバーも起動する（Web App/API 向け）
#   --skip-checks  ヘルスチェックをスキップ（デバッグ用）
# =============================================================================
set -euo pipefail

START_DEV=false
SKIP_CHECKS=false

for arg in "$@"; do
  case "$arg" in
    --dev) START_DEV=true ;;
    --skip-checks) SKIP_CHECKS=true ;;
  esac
done

echo "=== Session Startup ==="

# 1. 作業ディレクトリ確認
[ -d ".git" ] || { echo "ERROR: Not in git repository"; exit 1; }

# 1.5. 言語検出と必須ツールの自動インストール
echo "=== Tool auto-install ==="
if [ -f "go.mod" ]; then
  echo "Detected: Go"
  command -v golangci-lint &>/dev/null || { echo "Installing golangci-lint..."; go install github.com/golangci/golangci-lint/cmd/golangci-lint@latest 2>/dev/null || echo "WARN: golangci-lint install failed"; }
  command -v gofumpt &>/dev/null || { echo "Installing gofumpt..."; go install mvdan.cc/gofumpt@latest 2>/dev/null || echo "WARN: gofumpt install failed"; }
fi
if [ -f "pyproject.toml" ]; then
  echo "Detected: Python"
  command -v ruff &>/dev/null || { echo "Installing ruff..."; pip install ruff 2>/dev/null || echo "WARN: ruff install failed"; }
fi
if [ -f "package.json" ]; then
  echo "Detected: TypeScript/JavaScript"
  command -v oxlint &>/dev/null || { echo "Installing oxlint..."; npm install -g oxlint 2>/dev/null || echo "WARN: oxlint install failed"; }
  npx biome --version &>/dev/null 2>&1 || { echo "Installing biome..."; npm install -g @biomejs/biome 2>/dev/null || echo "WARN: biome install failed"; }
fi
if [ -f "Cargo.toml" ]; then
  echo "Detected: Rust"
  rustup component add clippy rustfmt 2>/dev/null || echo "WARN: rustup component install failed"
fi
# lefthook（全言語共通: git hooks 管理）
command -v lefthook &>/dev/null || { echo "Installing lefthook..."; go install github.com/evilmartians/lefthook@latest 2>/dev/null || npm install -g lefthook 2>/dev/null || echo "WARN: lefthook install failed"; }
echo "Tool check complete."

# 2. Gitログ読取（記述的コミットメッセージがセッション間ブリッジとして機能）
echo "=== Recent commits ==="
git log --oneline -10

# 3. CONTEXT.json — 現在の目標と次のアクション
echo "=== Current context ==="
if [ -f "CONTEXT.json" ]; then
  echo "Stage: $(jq -r '.current_stage // "unknown"' CONTEXT.json)"
  echo "Goal: $(jq -r '.goal // "not set"' CONTEXT.json)"
  echo "Next action: $(jq -r '.next_best_action // "not set"' CONTEXT.json)"
  echo "Last checks: $(jq -r '.last_checks // "no data"' CONTEXT.json)"
else
  echo "WARN: CONTEXT.json not found. Creating from template..."
fi

# 4. progress.json — セッション履歴と直近の状態
echo "=== Session history ==="
if [ -f "progress.json" ]; then
  TOTAL_SESSIONS=$(jq '.sessions | length' progress.json)
  echo "Total sessions: $TOTAL_SESSIONS"
  if [ "$TOTAL_SESSIONS" -gt 0 ]; then
    echo "Last session:"
    jq '.sessions[-1] | {ended_at, stage, summary, checks, next_action}' progress.json
  fi
else
  echo "INFO: progress.json not found. This appears to be the first session."
fi

# 5. ヘルスチェック
if [ "$SKIP_CHECKS" = true ]; then
  echo "=== Health check SKIPPED (--skip-checks) ==="
else
  echo "=== Health check ==="
  if make check 2>&1 | tail -10; then
    echo "All checks passed. Ready to work."
  else
    echo "WARN: Checks failed. Review issues before proceeding."
  fi
fi

# 6. 開発サーバー起動（オプション）
if [ "$START_DEV" = true ]; then
  echo "=== Starting dev server ==="
  if [ -f "package.json" ]; then
    npm run dev &
    echo "Dev server started (npm run dev)"
  elif [ -f "Makefile" ] && grep -q "run-dev" Makefile; then
    make run-dev &
    echo "Dev server started (make run-dev)"
  else
    echo "WARN: No dev server configuration found"
  fi
fi

# 7. セッション開始時刻を環境変数として記録（session-end.sh で使用）
export SESSION_START_TIME=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
echo ""
echo "=== Session started at $SESSION_START_TIME ==="
echo "Run 'bash session-end.sh \"summary\" \"next action\"' when done."
