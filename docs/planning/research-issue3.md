# Research: Issue #3 — Episode CRUD gRPC サービス実装

## 1. Quarkus gRPC サービス実装パターン

### proto ファイル配置
- Quarkus の `quarkus-grpc` 拡張は `src/main/proto` に配置された `.proto` ファイルを自動検出
- ビルド時に Java/gRPC スタブを自動生成（`mvn compile` / `gradlew build`）
- 外部の `proto/` ディレクトリにある定義を使うには、`src/main/proto` にシンボリックリンクまたはコピーする必要がある

### Quarkus gRPC の実装方式（2 パターン）

**Mutiny API（推奨）**
```kotlin
@GrpcService
class EpisodeGrpcService : MutinyEpisodeServiceGrpc.EpisodeServiceImplBase() {
    override fun createEpisode(request: CreateEpisodeRequest): Uni<CreateEpisodeResponse> {
        // Uni を返す非同期パターン
    }
}
```

**StreamObserver API（従来型）**
```kotlin
@GrpcService
class EpisodeGrpcService : EpisodeServiceGrpc.EpisodeServiceImplBase() {
    override fun createEpisode(request: CreateEpisodeRequest, observer: StreamObserver<CreateEpisodeResponse>) {
        // StreamObserver パターン
    }
}
```

### 結論
- Mutiny API を採用（Quarkus 推奨、Kotlin coroutines とも相性が良い）
- `@GrpcService` アノテーションで CDI Bean として自動登録

## 2. ステータス遷移ルール

### 遷移図
```
PLANNING ──→ GUEST_COORDINATION ──→ RECORDING ──→ EDITING ──→ REVIEW ──→ PUBLISHED
    │                                                           │
    └──────────→ RECORDING（ソロ収録）                          └──→ EDITING（修正依頼）
```

### 許可される遷移
| 現在のステータス       | 遷移先                                     |
|-----------------------|-------------------------------------------|
| PLANNING              | GUEST_COORDINATION, RECORDING             |
| GUEST_COORDINATION    | RECORDING                                 |
| RECORDING             | EDITING                                   |
| EDITING               | REVIEW                                    |
| REVIEW                | EDITING（修正依頼）, PUBLISHED             |
| PUBLISHED             | （遷移不可 — 最終ステータス）                |

### バリデーション実装
- `EpisodeStatus` に `canTransitionTo(target: EpisodeStatus): Boolean` メソッドを追加
- 不正遷移時は `io.grpc.StatusException(Status.INVALID_ARGUMENT)` をスロー

## 3. gRPC エラーハンドリング

### StatusException の使用
```kotlin
throw StatusException(Status.NOT_FOUND.withDescription("Episode not found: $id"))
throw StatusException(Status.INVALID_ARGUMENT.withDescription("Invalid status transition"))
throw StatusException(Status.ALREADY_EXISTS.withDescription("Episode already exists"))
```

### エラーコードマッピング
| ケース                | gRPC Status          |
|----------------------|---------------------|
| エピソード未発見       | NOT_FOUND           |
| 不正な入力            | INVALID_ARGUMENT    |
| 不正なステータス遷移   | INVALID_ARGUMENT    |
| title が空            | INVALID_ARGUMENT    |
| 内部エラー            | INTERNAL            |

## 4. テスト戦略

### H2 インメモリDB（既存設定を活用）
- `src/test/resources/application.properties` で H2 設定済み
- `quarkus.hibernate-orm.database.generation=drop-and-create` でテストごとにスキーマ再作成
- Flyway は無効（`quarkus.flyway.migrate-at-start=false`）

### テスト分類

**ユニットテスト: ステータス遷移ロジック**
- 純粋な Kotlin テスト（Quarkus 不要）
- EpisodeStatus の遷移マトリクスを検証

**統合テスト: gRPC CRUD**
- `@QuarkusTest` + `@GrpcClient` でサービスを呼び出し
- H2 インメモリDB で DB 操作を検証

### Quarkus gRPC テストパターン
```kotlin
@QuarkusTest
class EpisodeGrpcServiceTest {
    @GrpcClient
    lateinit var client: EpisodeService  // Mutiny 生成クライアント

    @Test
    fun `create episode`() {
        val response = client.createEpisode(request).await().indefinitely()
        // アサーション
    }
}
```

## 5. proto-to-Kotlin データ変換

### エンティティ ↔ proto メッセージ変換
- `Episode`（JPA エンティティ） ↔ `Episode`（proto メッセージ）の変換ヘルパーが必要
- Mapper クラスで双方向変換を提供
- `Instant` ↔ `google.protobuf.Timestamp` 変換
- `UUID` ↔ `String` 変換
- `EpisodeStatus`（Kotlin enum） ↔ `EpisodeStatus`（proto enum）変換
