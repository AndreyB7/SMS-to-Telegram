# SMS → Telegram

A small Android app that catches incoming SMS and forwards the **sender + text**
to a chosen Telegram chat via the Bot API. You only enter the **bot token** — the
chat is detected automatically with the **“Find chat”** button.

For personal use on **your own** device only.

> **⚠️ Keep the app running in the background.** It forwards SMS only while it stays
> in the background (minimized). If you **swipe it away** from Recents (fully close it),
> Android puts it in a stopped state and no longer delivers SMS to it until you open the
> app again. Don’t close it, and turn off battery optimization (and enable Autostart on
> MIUI/EMUI/etc.) so the system keeps it alive.

> 🇷🇺 [Русская версия ниже](#sms--telegram-русская-версия)

---

## ⚠️ Install the APK — read first (Google Play Protect)

This app **reads sensitive data — your incoming SMS**. Google Play Protect blocks
or warns about sideloaded apps that request SMS access, because that permission is
common in malware. This is expected: you built and signed the app yourself, it only
forwards new incoming messages and never reads your SMS history.

Download the APK from the **[latest release](https://github.com/AndreyB7/SMS-to-Telegram/releases/latest)**,
then install it with one of the options below.

**Option A — Install anyway (simplest):**
In the Play Protect dialog tap **More details** → **Install anyway**
(may be labelled *Install without scanning*).

**Option B — Temporarily turn off Play Protect:**
1. Open **Play Store** → your profile picture → **Play Protect**.
2. ⚙️ **Settings** → turn **off** *Scan apps with Play Protect*.
3. Install the APK.
4. Turn scanning back **on** afterwards.

**Option C — Install via adb (Play Protect doesn’t interfere):**
```bash
adb install -r app-release.apk
```

---

## 1. Create a Telegram bot (no code, ~2 min)

1. In Telegram open **@BotFather** → `/newbot` → set a name.
   You’ll get a **token** like `123456789:AAE...`.
2. Find your bot by name and send it any message (e.g. “hi”).

You don’t need to look up the chat_id manually — the app reads it from that
message with the **Find chat** button.

## 2. Configure on the device

1. Paste the **Bot token** (saved automatically — there is no Save button).
2. Open Telegram and send your bot any message.
3. Tap **Find chat** — the app detects the chat and shows its name.
   If you see “No messages found”, message the bot first (step 2) and tap again.
4. Grant the **SMS permission** (or use the “Request SMS permission” button).
5. Tap **Send test message** (enabled only after a chat is found) — the chat
   should receive “✅ Test: the app is connected to Telegram”.
6. Send yourself a real SMS from another phone to verify.

### Keep the app awake
Android may restrict background work. Open
**Settings → Apps → SMS to Telegram → Battery** and choose **Unrestricted**
(turn off battery optimization). Otherwise some SMS may not be forwarded while
the phone sleeps. The app also has an in-app **“Allow background work”** button.

## 3. Build from source (Android Studio)

1. **Android Studio** → `File → Open` → select the project folder.
2. Wait for **Gradle Sync** (Studio downloads Gradle 8.14.5, AGP and deps).
   Accept the SDK components it offers (compileSdk 34).
3. Connect a phone with **USB debugging** on, or start an emulator.
4. Press ▶ **Run 'app'**.

> Debug APK without running: `Build → Build Bundle(s)/APK(s) → Build APK(s)`
> → `app/build/outputs/apk/debug/app-debug.apk`.
> Signed release build: see `keystore.properties.template`, then
> `./gradlew assembleRelease` → `app/build/outputs/apk/release/app-release.apk`.

## How it works

| File | Purpose |
|------|---------|
| `SmsReceiver.kt` | `BroadcastReceiver` on `SMS_RECEIVED`: reads the SMS and sends it |
| `TelegramClient.kt` | `sendMessage` (send), `getMe` (health check), `getUpdates` (find chat) |
| `Prefs.kt` | Stores token, chat_id and chat name in `SharedPreferences` |
| `MainActivity.kt` | Settings screen, chat lookup, permission request, test |

**“Find chat”** calls `getUpdates` and takes `chat.id` from the latest message to
the bot — so message the bot at least once first (Telegram keeps updates ~24h and
only if no webhook is set).

## Permissions & privacy

- `RECEIVE_SMS` — receive incoming SMS (granted manually on first run).
- `INTERNET` — send to Telegram.

The app does **not** request `READ_SMS` and does not read your message history —
only new incoming messages at the moment they arrive. The bot token is kept in the
app’s private storage; don’t install the build on devices that aren’t yours.

## Notes / limitations

- Self-signed release APK for personal sideloading, not published on Google Play
  (Play heavily restricts SMS-permission apps).
- MMS are not handled — text SMS only (including long/multipart messages).
- minSdk 24 (Android 7+).

---
---

# SMS → Telegram (русская версия)

Небольшое Android-приложение: ловит входящие SMS и пересылает **отправителя + текст**
в заданный Telegram-чат через Bot API. В приложении указывается только **токен бота** —
чат определяется автоматически кнопкой **«Найти чат»**.

Только для личного использования на **своём** устройстве.

> **⚠️ Приложение должно оставаться в фоне.** Оно пересылает SMS только пока свёрнуто
> (работает в фоне). Если **смахнуть его** из списка недавних (полностью закрыть),
> Android переводит его в остановленное состояние и перестаёт доставлять ему SMS, пока
> ты снова не откроешь приложение. Не закрывай его и отключи оптимизацию батареи
> (а на MIUI/EMUI и т.п. включи «Автозапуск»), чтобы система его не выгружала.

---

## ⚠️ Установка APK — прочти сначала (Google Play Protect)

Приложение **читает чувствительные данные — твои входящие SMS**. Google Play Protect
блокирует или предупреждает при установке «сбоку» приложений с доступом к SMS, потому
что такое разрешение часто встречается у вредоносов. Это ожидаемо: ты сам собрал и
подписал приложение, оно пересылает только новые входящие сообщения и не читает
историю переписки.

Скачай APK из **[последнего релиза](https://github.com/AndreyB7/SMS-to-Telegram/releases/latest)**
и установи одним из способов ниже.

**Вариант A — «Установить всё равно» (проще всего):**
В окне Play Protect нажми **«Подробнее»** → **«Всё равно установить»**
(может называться *«Установить без проверки»*).

**Вариант B — временно отключить Play Protect:**
1. Открой **Play Маркет** → аватарка профиля → **Play Защита**.
2. ⚙️ **Настройки** → выключи **«Проверять приложения»**.
3. Установи APK.
4. После установки **включи проверку обратно**.

**Вариант C — установка через adb (Play Protect не вмешивается):**
```bash
adb install -r app-release.apk
```

---

## 1. Подготовить Telegram-бота (без кода, ~2 минуты)

1. В Telegram открой **@BotFather** → `/newbot` → задай имя.
   Получишь **токен** вида `123456789:AAE...`.
2. Найди своего бота по имени и напиши ему любое сообщение (например «привет»).

chat_id вручную искать не нужно — приложение возьмёт его из этого сообщения
кнопкой **«Найти чат»**.

## 2. Настроить на устройстве

1. Вставь **Bot token** (сохраняется автоматически, отдельной кнопки нет).
2. Открой Telegram и напиши своему боту любое сообщение.
3. Нажми **«Найти чат»** — приложение определит чат и покажет его имя.
   Если появится «Сообщения не найдены» — сначала напиши боту (шаг 2) и нажми снова.
4. Выдай разрешение **на приём SMS** (или кнопкой «Запросить разрешение»).
5. Нажми **«Отправить тестовое сообщение»** (кнопка активна только после того,
   как найден чат) — в чате должно появиться «✅ Тест: приложение подключено к Telegram».
6. Отправь себе настоящую SMS с другого телефона для проверки.

### Чтобы приложение не «засыпало»
Android может ограничивать фоновую работу. Открой
**Настройки → Приложения → SMS to Telegram → Батарея** и выбери **«Без ограничений»**
(отключи оптимизацию батареи). Иначе часть SMS может не пересылаться, пока телефон
спит. В приложении есть и кнопка **«Разрешить работу в фоне»**.

## 3. Собрать из исходников (Android Studio)

1. **Android Studio** → `File → Open` → выбери папку проекта.
2. Дождись **Gradle Sync** (Studio сама скачает Gradle 8.14.5, AGP и зависимости).
   Согласись установить нужные компоненты SDK (compileSdk 34).
3. Подключи телефон с **отладкой по USB** или запусти эмулятор.
4. Нажми ▶ **Run 'app'**.

> Debug-APK без запуска: `Build → Build Bundle(s)/APK(s) → Build APK(s)`
> → `app/build/outputs/apk/debug/app-debug.apk`.
> Подписанный release: см. `keystore.properties.template`, затем
> `./gradlew assembleRelease` → `app/build/outputs/apk/release/app-release.apk`.

## Как это устроено

| Файл | Назначение |
|------|-----------|
| `SmsReceiver.kt` | `BroadcastReceiver` на `SMS_RECEIVED`: читает SMS, запускает отправку |
| `TelegramClient.kt` | `sendMessage` (отправка), `getMe` (проверка связи), `getUpdates` (поиск чата) |
| `Prefs.kt` | Хранение токена, chat_id и имени чата в `SharedPreferences` |
| `MainActivity.kt` | Экран настроек, поиск чата, запрос разрешения, тест |

**«Найти чат»** вызывает `getUpdates` и берёт `chat.id` из последнего сообщения боту —
поэтому сначала напиши боту хотя бы раз (Telegram хранит апдейты ~24 ч и только если
боту не задан webhook).

## Разрешения и приватность

- `RECEIVE_SMS` — приём входящих SMS (выдаётся вручную при запуске).
- `INTERNET` — отправка в Telegram.

Приложение **не** запрашивает `READ_SMS` и не читает историю переписки — только новые
входящие сообщения в момент получения. Токен хранится в приватных настройках
приложения; не ставь сборку на чужие устройства.

## Ограничения / на что обратить внимание

- Самоподписанный release-APK для личного sideload, не для публикации в Google Play
  (Play сильно ограничивает приложения с доступом к SMS).
- MMS не обрабатываются — только текстовые SMS (в т.ч. длинные/многочастные).
- minSdk 24 (Android 7+).
