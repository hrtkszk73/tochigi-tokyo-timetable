# 栃木⇔東京 時刻表アプリ

両毛線＋東北新幹線（やまびこ・なすの）を組み合わせた、栃木⇔東京の通勤・帰省用Androidアプリ。
アプリを開くと「いまから乗れる次の電車」と以降3便を一画面で確認できる。

## できること

- 「東京へ行く」「栃木へ帰る」をタブで切替
- 現在時刻 + 駅までの徒歩・移動時間 を考慮して "次に乗れる" 便をハイライト
- 全区間表示：東京発 → 小山着 → 小山発 → 栃木着（と逆方向）と乗換待ち時間が一目で見える
- 「駅まで何分」は設定画面で東京駅・栃木駅それぞれ保存（DataStore）
- 平日ダイヤのみ収録。土日祝は「未対応」の警告画面に切替
- Material 3 / Dynamic Color（Android 12+）、ライト/ダーク自動

## データ

- `app/src/main/assets/timetable.json` に下り24便・上り24便を格納
- 出典: NAVITIME 公式時刻表（令和8年5月時点・平日ダイヤ）
- 小山駅の両毛線⇔新幹線ホーム間は4分以上の乗換時間を確保
- 新幹線は小山停車列車のみ（はやぶさ等の小山通過列車は対象外）

時刻表を更新したい場合は `timetable.json` を編集してコミットすればOK。

## ビルド方法（GitHub Actions）

このマシンには Android Studio / JDK を入れずに、GitHub Actions で APK を作る前提でセットアップしてある。

### 初回セットアップ

1. **GitHubでリポジトリを作成**
   - 例: `tochigi-tokyo-timetable`（Private で良い）

2. **このフォルダをコミットして push**
   ```powershell
   cd C:\Users\hirotaka.suzuki\MyProject\TochigiTokyoTimetable
   git init
   git add .
   git commit -m "initial commit"
   git branch -M main
   git remote add origin https://github.com/<あなたのID>/tochigi-tokyo-timetable.git
   git push -u origin main
   ```

3. **Actions が自動で走る**
   - リポジトリの `Actions` タブを開く
   - `Build Android APK` ワークフローが緑になるのを待つ（初回約3〜5分）

4. **APK をダウンロード**
   - 成功したワークフローの実行ページを開き、`Artifacts` セクションの `app-debug-apk` をダウンロード
   - zipを展開すると `app-debug.apk` が出てくる

### スマホへインストール

1. APK を Android スマホへ転送（USB、Google Drive、メール添付など）
2. スマホで APK を開く
3. 初回は「提供元不明のアプリのインストール」を許可する必要がある
   - 設定 → アプリ → 特別なアプリアクセス → 不明なアプリのインストール → 使った（ファイラ/Chrome等）アプリを「許可」
4. インストール完了。ホーム画面に「栃木⇔東京 時刻表」が表示される

### 時刻表やUIの更新

- `timetable.json` を編集 → push → Actions が新しい APK を作る → 再ダウンロードして上書きインストール
- 既存アプリの上書きインストールでは設定（駅までの所要時間）は保持される

## ローカルでビルドしたくなったら（オプション）

Android Studio を入れる場合:

1. [Android Studio](https://developer.android.com/studio) をダウンロードしてインストール
2. Studio で `TochigiTokyoTimetable` フォルダを Open
3. 初回 Gradle Sync が走る（数分）
4. 上部の Run ▶ で実機/エミュレータ実行

## プロジェクト構成

```
TochigiTokyoTimetable/
├── app/
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── assets/timetable.json        ← 時刻表データ
│       ├── java/com/example/timetable/
│       │   ├── MainActivity.kt
│       │   ├── data/                    ← データクラス・JSON読込・DataStore
│       │   └── ui/                      ← Compose UI (MainScreen, SettingsDialog, Theme, ViewModel)
│       └── res/                         ← strings, themes, icon
├── build.gradle.kts                     ← ルートビルド設定
├── app/build.gradle.kts                 ← アプリビルド設定
├── settings.gradle.kts
├── gradle.properties
└── .github/workflows/android.yml        ← GitHub Actions
```

## ライセンス・配布の注意

- 個人利用前提。時刻表データの著作権は元の鉄道事業者・NAVITIMEに帰属する点に注意
- Google Play への配布は想定していない
- 配布する場合は release ビルドの署名（keystore）が必要
