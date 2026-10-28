# FW/API/MCP演習 成果物 — Todoアプリ

## 1. アプリ概要

JavaのSpring Bootを使用して開発したTodo管理Webアプリです。

Todoの登録・編集・削除などの基本機能に加え、カレンダー表示やREST API、MCP連携などを実装しています。

### 主な機能

- Todoの登録・編集・削除
- Todoの完了状態の管理
- Todoの検索、期限順の並び替え
- 優先度・ジャンル・期限の管理
- Todoの印付け
- 削除済みTodoの確認・復元
- 月間・週間カレンダー表示
- REST APIによるTodo操作
- MCPツールによるTodo操作
- CSV出力

## 2. 使用技術

| 項目 | 技術 |
|---|---|
| 言語 | Java 21 |
| フレームワーク | Spring Boot |
| HTMLテンプレート | Thymeleaf |
| データベース | MySQL 8.0 |
| DBアクセス | MyBatis |
| ビルド | Maven |
| 実行環境 | Docker / Docker Compose |

## 3. 起動前の準備

以下の環境が必要です。

- Docker Desktop
- Git
- Webブラウザ（Chromeなど）

Docker Desktopを起動し、Docker Engineが動作していることを確認してください。

※ Dockerを使用してアプリ全体を起動する場合、ホストPCへのJava・MySQLの個別インストールは不要です。

## 4. ダウンロード

GitHubのリポジトリページから、以下の操作でダウンロードしてください。

1. 「Code」をクリック
2. 「Download ZIP」をクリック
3. ZIPファイルを展開
4. 展開したプロジェクトフォルダを開く

## 5. 環境変数の設定

プロジェクト直下に `.env.example` があります。

このファイルをコピーし、同じフォルダに `.env` という名前で保存してください。

`.env` に、任意のMySQL rootパスワードを設定します。

記載例：

```env
MYSQL_ROOT_PASSWORD=任意のパスワード
```

※ 上記は設定形式の例です。実際には任意のパスワードを設定してください。

※ `.env` はローカル環境用の設定ファイルです。GitHubへのアップロードは不要です。

## 6. アプリの起動

プロジェクト直下でターミナルを開き、以下のコマンドを実行してください。

```powershell
docker compose --profile docker-app up -d --build
```

初回起動時はDockerイメージのダウンロードやビルドが行われます。

MySQLのヘルスチェックが正常になってから、Spring Bootアプリが起動します。

### 起動状態の確認

```powershell
docker compose --profile docker-app ps
```

コンテナが正常に起動していることを確認してください。

## 7. ブラウザで確認

アプリ起動後、以下のURLにアクセスしてください。

**Todo一覧画面**

http://localhost:8080/todos

**カレンダー画面**

http://localhost:8080/calendar

**Todo API**

http://localhost:8080/api/todos

※ 8080番・3306番ポートを別のアプリが使用している場合、起動時に競合する可能性があります。

## 8. アプリの停止

確認終了後、プロジェクト直下で以下を実行してください。

```powershell
docker compose --profile docker-app down
```

このコマンドでは、通常、データ保存用のDockerボリュームは削除されません。

## 9. 補足事項

- データベース名は `todoapp` です。
- MySQLは3306番ポート、Webアプリは8080番ポートを使用します。
- 初回のデータベース初期化用SQLは `initdb` フォルダに配置しています。
- データベースの保存にはDockerボリューム `fw-db-data` を使用しています。
- MCP連携の実装コードは `src/main/java/com/example/todoapp/mcp` にあります。

---

FW/API/MCP演習 提出成果物
