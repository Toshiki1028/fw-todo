# DB設計書：やること管理アプリ

## 1. 対象と参照元

DB（データの保存先）にはMySQLを使う。テーブル（データを行と列で保存する表）は `todos` の1つだけとし、やること1件を1行に保存する。

この設計書は `requirements.md` の「2. 作る機能」「3. 保存する情報」「5. 入力と確認の決まり」「6. 一覧・検索・並び替え」「7. 操作結果と記録」と、`handout_app_spec.md` の「2. 保存するもの」「5. 決まりごと」に対応する。画面の配置や見た目は、この設計書の対象に含めない。

## 2. `todos` テーブルのカラム

カラムは表の列、型は保存できる値の種類を表す。「NULL可」は、値を入れなくてもよいことを意味する。

| カラム | 型 | NULL可 | 自動入力・既定値 | 対応する要件 |
|---|---|---|---|---|
| `id` | `BIGINT` | 不可 | 1ずつ自動採番 | 1件の識別、編集・削除の対象指定、操作ログに残す番号。主キー（重複しない識別番号）。 |
| `title` | `VARCHAR(255)` | 不可 | なし | 必須の「やること」。一覧表示と、文字の一部を含むかによる検索。255文字以内。 |
| `detail` | `VARCHAR(255)` | 可 | なし | 省略できる「メモ」。255文字以内。 |
| `category` | `VARCHAR(255)` | 不可 | なし | 必須のジャンル。指定の5種類から選び、一覧表示と完全一致の絞り込みに使う。 |
| `priority` | `INT` | 不可 | `2`（中） | 優先度。`1`＝高、`2`＝中、`3`＝低。登録・編集で選び、一覧に表示する。 |
| `due_date` | `DATE` | 可 | なし | 省略できる期限。一覧表示と、近い順・遠い順の並び替えに使う。 |
| `completed` | `BOOLEAN` | 不可 | `FALSE`（未完了） | 完了状態。`1`＝完了、`0`＝未完了。編集で変更し、一覧に表示する。 |
| `created_at` | `DATETIME` | 不可 | 登録時にMySQLが現在日時を設定 | 登録した日時。利用者の入力欄には出さない。 |
| `updated_at` | `DATETIME` | 不可 | 登録時と更新時にMySQLが現在日時を設定 | 最後に直した日時。利用者の入力欄には出さない。 |

`category` に入る値は「デザイン」「マーケティング」「プログラミング」「資格」「就職活動」の5種類とする。`id`・`created_at`・`updated_at` は利用者が入力しない。

## 3. CREATE TABLE 文（DDL）

DDLは、DBに表の形を作るためのSQL文（DBへの命令文）。次の文で `todos` を作る。

```sql
CREATE TABLE todos (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    detail VARCHAR(255) NULL,
    category VARCHAR(255) NOT NULL,
    priority INT NOT NULL DEFAULT 2,
    due_date DATE NULL,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) DEFAULT CHARACTER SET utf8mb4;
```

`AUTO_INCREMENT` は番号を1ずつ増やす指定、`NOT NULL` は値を省略できない指定、`DEFAULT` は値が渡されない場合の既定値。`ON UPDATE CURRENT_TIMESTAMP` は行の更新時に `updated_at` を現在日時にする指定である。`utf8mb4` は日本語を保存するための文字コード（文字の保存方法）として指定する。

## 4. 入力値とDBの役割分担

- 登録画面では `priority` を必ず選ばせ、最初は空欄にする。DBの既定値 `2` は、値を指定せずに保存した場合の決まりであり、画面の初期選択には使わない。
- `title` の空文字や半角・全角の空白だけの入力は、DBの `NOT NULL` だけでは防げない。登録・編集の入力確認で止める。
- `category` の5種類、`priority` の1～3という選択肢は、登録・編集時に守る。上のDDLでは、これらの値の種類そのものをDB側で制限していない。
- `detail` と `due_date` は省略できる。`completed` は新規登録時に未完了を既定とし、編集で変更できる。
- 存在しない `id` を指定したときの一覧への移動と「見つかりませんでした」の表示は、DBの表ではなくアプリ側の処理とする。
- 操作ログ（動作の記録）は登録・編集・削除の種類と `id` を記録する。`title` と `detail` の中身は記録しない。ログ用の別テーブルは作らない。

## 5. 参照元にない設計上の指定

DDLに `DEFAULT CHARACTER SET utf8mb4` を指定した。日本語のジャンルや「やること」を保存するための指定であり、参照元には文字コードの指定はない。それ以外のカラム・型・必須／省略可・既定値は参照元に従う。
