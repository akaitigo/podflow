# ADR-003: gRPC-Web 方式の選定（Connect vs Envoy）

## ステータス

Accepted

## コンテキスト

podflow はフロントエンド（React SPA）とバックエンド（Quarkus gRPC）間の通信に gRPC-Web を採用する。ブラウザは native gRPC（HTTP/2 フレーミング）を直接扱えないため、ブラウザ互換の通信方式を選定する必要がある。

候補は以下の 2 つ:

1. **Connect Protocol（Buf Connect）**: ブラウザから直接呼び出せる HTTP/1.1 ベースの gRPC 互換プロトコル。Envoy 等のプロキシが不要。
2. **Envoy gRPC-Web proxy**: Envoy をリバースプロキシとして配置し、gRPC-Web ↔ gRPC の変換を行う。

### 評価軸

| 観点 | Connect | Envoy |
|------|---------|-------|
| デプロイ複雑度 | 低（プロキシ不要） | 高（Envoy の設定・運用が必要） |
| レイテンシ | 直接通信で低い | プロキシ経由で +1hop |
| エコシステム成熟度 | 比較的新しいが Buf 社が積極開発 | 実績豊富 |
| Quarkus 対応 | Quarkus Connect 拡張あり（quarkus-grpc-web） | Envoy の設定が必要 |
| フロントエンドSDK | `@connectrpc/connect-web` で型安全 | `grpc-web` パッケージ（Google 公式だがメンテ頻度低） |
| ストリーミング | Unary + Server streaming サポート | フルサポート |
| 本番実績 | 採用増加中、Buf 社プロダクトで実証 | 大規模実績多数 |

## 決定

**Connect Protocol を採用する。**

理由:

1. **デプロイ簡素化**: Envoy を挟まないため、Cloud Run へのデプロイがシンプル。MVP フェーズでインフラ運用コストを最小化できる。
2. **開発体験**: `@connectrpc/connect-web` + `buf generate` で Proto 定義からフロントエンド SDK が自動生成され、型安全性が高い。
3. **Quarkus 互換性**: Quarkus は `quarkus-grpc` 拡張で Connect プロトコルのリクエストを処理可能。追加設定で `application/connect+proto` を受け付ける。
4. **段階的採用**: MVP ではモック API で開発し、バックエンド接続時に Connect クライアントに差し替える設計になっている（`EpisodeApi` インターフェースで抽象化済み）。

## リスク

- Connect Protocol は Envoy gRPC-Web と比べて本番実績が少ない。大規模トラフィック時に未知の問題が発生する可能性がある。
  - **軽減策**: MVP のトラフィック規模（100 ユーザー）では問題にならない。スケール時に Envoy への移行パスも確保（Connect は gRPC 互換のため、サーバー側の変更なく切り替え可能）。
- Bidirectional streaming は Connect ではサポートされない。
  - **軽減策**: podflow のユースケースは Unary RPC が主体であり、streaming の必要性は低い。将来必要になった場合は WebSocket またはサーバーサイドイベントで補完する。

## 結果

- フロントエンド → バックエンド間通信は Connect Protocol（HTTP/1.1）を使用
- `@connectrpc/connect-web` をフロントエンド SDK として使用
- Envoy プロキシは不要。Cloud Run 上で Quarkus が直接 Connect リクエストを処理
- `EpisodeApi` インターフェースにより、モック API から Connect クライアントへの切り替えが容易
