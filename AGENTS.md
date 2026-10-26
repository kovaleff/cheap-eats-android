# AGENTS.md

Android-приложение «Карта нищеброда» (Jetpack Compose) — нативный аналог веб-версии. Спецификация API бэкенда: `/home/kovaleff/www/cheap-eats/.ai/api-for-andoid.md` — читать перед работой с сетевым слоем.

## Project Overview

Kotlin + Jetpack Compose клиент для Laravel-бэкенда «Карта нищеброда» (карта дешёвых заведений). Веб-версия: `/home/kovaleff/www/cheap-eats`, доступна как `https://cheap-eats`. Русскоязычный UI, тёмная фирменная тема.

## Tech Stack

- **Kotlin 2.2.10, AGP 9.3.3, Gradle 9.5 (wrapper), compileSdk 36, minSdk 24**
- **UI**: Jetpack Compose (BOM 2026.02.01), Material 3, material-icons-extended
- **Навигация**: androidx.navigation-compose (`home` и `map?categoryId={id}`)
- **Сеть**: Retrofit + kotlinx-serialization (не Moshi/GSON), OkHttp 4
- **Карта**: osmdroid 6.1.20 + тайлы OpenStreetMap (MAPNIK)
- **Картинки**: Coil 2.7
- **DI**: нет (объекты-синглтоны `ApiClient`/`CheapEatsRepository`) — при росте проекта рассмотреть Hilt
- Версии — в `gradle/libs.versions.toml` (version catalog)

## Commands

```bash
# Сборка debug APK
./gradlew :app:assembleDebug

# Установка и запуск на эмуляторе (эмулятор должен быть запущен)
/mnt/dockerDisk/Progs/Android/Sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
/mnt/dockerDisk/Progs/Android/Sdk/platform-tools/adb shell am start -n com.kovaleff.cheapeats/.MainActivity
```

## Architecture

```
app/src/main/java/com/kovaleff/cheapeats/
├── MainActivity.kt              # NavHost: home / map
├── CheapEatsApplication.kt      # ImageLoader для Coil (общий OkHttp с devDns)
├── data/
│   ├── api/                     # Retrofit-интерфейсы + ApiConfig (URL, UA)
│   ├── model/                   # DTO (kotlinx-serialization), ApiDtos.kt — запросы
│   ├── remote/ApiClient.kt      # OkHttp + Retrofit-фабрики, devDns
│   └── repository/CheapEatsRepository.kt  # вся бизнес-логика (Overpass, sync, geocode)
└── ui/
    ├── home/                    # HomeScreen + HomeViewModel
    ├── map/                     # MapScreen, MapViewModel, CheapEatsMapView (osmdroid)
    └── theme/                   # брендовые цвета веб-версии, тёмная тема
```

ViewModel через `viewModel()`, состояние — `StateFlow<UiState>`. Сетевые вызовы только из `viewModelScope.launch` через репозиторий.

## Environment (важно)

- **Android SDK**: `/mnt/dockerDisk/Progs/Android/Sdk` (прописан в `local.properties`).
- **Эмулятор**: AVD `medium_phone` (Android 15, google_apis_playstore, x86_64). Данные AVD лежат в `/mnt/oldWin/AVD/medium_phone.avd` (указатель — `~/.android/avd/medium_phone.ini`). Запуск: `$ANDROID_HOME/emulator/emulator -avd medium_phone -no-window -gpu swiftshader_indirect`.
- **Gradle/кэши**: держать на дисках со свободным местом; в `/home` мало места (AVD оттуда перенесён).
- **Разрешения локации**: на эмуляторе выдавать `pm grant ... ACCESS_FINE_LOCATION` и `ACCESS_COARSE_LOCATION`; GPS ставить `adb emu geo fix 38.9898 45.0197` (Краснодар).

## Gotchas

- **dev-домен `cheap-eats` не резолвится из эмулятора** (он только в `/etc/hosts` хоста). Решение — кастомный `Dns` в `ApiClient.kt`: `cheap-eats` → `10.0.2.2`, остальные хосты через системный DNS. SNI и проверка сертификата остаются на оригинальном имени. Coil обязан использовать тот же клиент (`CheapEatsApplication.newImageLoader`), иначе картинки не грузятся.
- **TLS**: сертификат сервера — mkcert. Корневой CA вшит: `app/src/main/res/raw/mkcert_ca.pem` + `res/xml/network_security_config.xml` (домен `cheap-eats`). Если сертификат перевыпущен — обновить PEM в res/raw.
- **Overpass API**: зеркала капризны. Regex с `|` в запросе (`amenity~"cafe|fast_food|restaurant"`) даёт 500 на зеркалах за Apache — использовать отдельные statements по каждому `amenity="..."` (как в `CheapEatsRepository.fetchOverpassElements`). Ретраи: 2 круга по 3 зеркалам (`overpass-api.de` → `kumi.systems` → `private.coffee`). 502/500 от зеркал — временные, повторять позже, это не баг приложения.
- **`sync-overpass` требует живой MySQL на хосте**: при `SQLSTATE[HY000] [2002] Connection refused` — `sudo systemctl start mysql` на хосте.
- **Пины osmdroid**: hitTest работает по битмапу иконки (96px), хитбокс маленький — тапать по кружку пина точно. Увеличить при необходимости размер в `makePinDrawable`.
- **Координаты тапов через adb** — в реальных пикселях экрана (1080×2400), а не в координатах скриншота.
- **`is_default` у City приходит как int 0/1**, не boolean.
- **`average_check` — строка** ("250.00", decimal:2). `osm_id` в путях API — строка.
- Категории/города на бэке кэшируются на сутки — правки в админке появляются не сразу.
- `exists: false` у venue — заглушка из OSM (в UI помечается «Не проверено»).

## Conventions

- Комментарии в коде — на русском, описывают ограничения/причины, а не «что делает строка».
- Ссылки на соответствующие места веб-версии в комментариях (`как в resources/js/services/overpass.js`) — помогают держать паритет.
- Имена по бренду веба: цвета в `ui/theme/Color.kt` дублируют `public/css/site.css`.
- Новые эндпоинты добавлять в `data/api/CheapEatsApiService.kt` + DTO в `data/model/`, логика — только в репозитории.
