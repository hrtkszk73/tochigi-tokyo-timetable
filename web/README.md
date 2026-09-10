# 栃木⇔東京 時刻表 PWA

Android 版と同じ機能を Web 技術で実装した Progressive Web App。iPhone を含むあらゆる端末で
ブラウザから開いて「ホーム画面に追加」すればアプリのように使える。

## ホスティング

`main` に push すると GitHub Actions が [`.github/workflows/pages.yml`](../.github/workflows/pages.yml) 経由で
GitHub Pages にデプロイ。URL: <https://hrtkszk73.github.io/tochigi-tokyo-timetable/>

## 構造

- `index.html` — ヘッダ + タブ + `<main>` + 設定ダイアログ
- `styles.css` — Material 3 相当の見た目、ライト/ダーク自動、Safe-area 対応
- `app.js` — フレームワーク未使用のバニラJS。JSONを fetch → タブごとに DOM を組み立てる
- `manifest.webmanifest` — PWA マニフェスト（standalone 表示、アイコン、テーマ色）
- `sw.js` — Service Worker。アプリシェルをキャッシュ、`timetable.json` はネットワーク優先＋オフライン fallback
- `timetable.json` — 時刻表データ。CI がデプロイ時に `app/src/main/assets/timetable.json` の内容で上書き
- `icons/` — 192px / 512px / apple-touch (180px) / favicon

## 時刻表データの更新

**単一のソース**: `app/src/main/assets/timetable.json`

このファイルを編集して push すると、Android APK 用と Pages 用の両方に反映される
（Pages ジョブがデプロイ直前に `web/timetable.json` を上書きコピーする）。

## iPhone での使い方

1. Safari で <https://hrtkszk73.github.io/tochigi-tokyo-timetable/> を開く
2. 共有ボタン（□↑）→「ホーム画面に追加」
3. 「追加」でホーム画面にアイコン設置
4. 以降はアイコンからスタンドアロン起動、オフラインでも表示可能

## ローカル開発

`file://` 直開きは Service Worker が動かないので、簡易HTTPサーバー経由で:

```powershell
# Python が入っていれば
cd web
python -m http.server 8080
# → http://localhost:8080/
```

Node.js があれば `npx serve web` などでも可。
