# podflow — アーキテクチャ概要

## 設計判断
- モノレポ構成: frontend/ + backend/ + proto/ を1リポジトリで管理
- gRPC-Web: フロントエンド→バックエンド間通信に使用
- ADR: 重要な技術選定は docs/adr/ に記録

## 外部サービス連携
- GCP Cloud Storage: 音声ファイル保存
- 文字起こしAPI: 未選定（ADR予定）
- 認証: 未選定（ADR予定）
