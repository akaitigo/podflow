# ADR-005: JWT認証の導入

## ステータス

Accepted（ADR-001 を Superseded に変更）

## コンテキスト

podflow v1.0.0 は認証未実装で「本番公開禁止」状態にあった（ADR-001）。本番利用に向けて認証機能の実装が必要になった。

### 検討した選択肢

1. **Firebase Authentication**: Google エコシステムとの親和性が高いが、外部サービス依存。
2. **Keycloak**: セルフホスト型でカスタマイズ性が高いが、運用コストが大きい。
3. **Quarkus SmallRye JWT**: Quarkus ネイティブの JWT 実装。MicroProfile JWT 仕様準拠。追加インフラ不要。
4. **Auth0 / Clerk**: マネージドサービスで導入容易だが、従量課金が発生。

## 決定

**Quarkus SmallRye JWT による自前 JWT 認証を採用する。**

理由:

1. **インフラ追加不要**: Quarkus のビルトイン拡張であり、外部認証サーバーが不要。
2. **gRPC インターセプターとの親和性**: ADR-001 で設計済みの gRPC インターセプター方式とそのまま統合できる。
3. **コスト**: 従量課金が一切発生しない。
4. **MicroProfile JWT 準拠**: 標準仕様に準拠しているため、将来的に OIDC プロバイダーへの移行も容易。

## アーキテクチャ

### 認証フロー

```
[Frontend] --POST /auth/login--> [Backend REST] --JWT発行--> [Frontend]
[Frontend] --gRPC + Authorization: Bearer <token>--> [gRPC Interceptor] --検証OK--> [gRPC Service]
```

### コンポーネント

- **AuthResource (REST)**: `/auth/login`, `/auth/register` エンドポイント
- **GrpcAuthInterceptor**: gRPC GlobalInterceptor。Authorization メタデータからトークンを検証
- **JwtTokenService**: SmallRye JWT Build でトークン生成
- **PasswordHasher**: bcrypt によるパスワードハッシュ化
- **AuthProvider (Frontend)**: React Context でトークン管理。localStorage に永続化
- **LoginPage (Frontend)**: ログイン/登録 UI

### セキュリティ

- パスワードは bcrypt (cost=12) でハッシュ化して保存
- JWT は RSA (RS256) で署名。秘密鍵はサーバーのみ保持
- トークンの有効期限はデフォルト 60 分（環境変数で設定可能）
- `/health` と `/auth/*` は認証不要
- その他の全エンドポイント（gRPC 含む）は認証必須

## リスク

- リフレッシュトークン未実装: 現在はトークン期限切れ時に再ログインが必要。将来的にリフレッシュトークンの導入を検討。
- セッション管理: サーバーサイドのセッション管理（トークン無効化）は未実装。即座のアカウント停止が必要な場合は別途対応が必要。

## 結果

- 全 gRPC エンドポイントが認証必須になった
- フロントエンドにログイン画面と認証ガードが追加された
- README の「本番公開禁止」警告を更新
- ADR-001 のステータスを Superseded に変更
