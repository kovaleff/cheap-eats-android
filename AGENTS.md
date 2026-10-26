# AGENTS.md

Android-клиент «Карта нищеброда» (Jetpack Compose) — нативный аналог веб-версии. Бэкенд и веб лежат в `/home/kovaleff/www/cheap-eats` (dev-домен `https://cheap-eats`). Актуальная спецификация API — `/home/kovaleff/www/cheap-eats/.ai/api-for-andoid.md` (вне этого репозитория, читать перед работой с сетью).

## Сборка и проверка

- Один модуль `:app`. Kotlin 2.2.10, AGP 9.3.3, Gradle 9.5.0 (wrapper), демон на JDK 21 (`gradle/gradle-daemon-jvm.properties`), compileSdk 36.1 (`release(36) { minorApiLevel = 1 }`), targetSdk 36, minSdk 24, Java 11.
- Compose BOM 2026.02.01 + Material3; Retrofit + kotlinx-serialization; osmdroid 6.1.20 (MAPNIK); Coil 2.7. Версии — `gradle/libs.versions.toml`.
- AGP 9 включает Kotlin сам: плагин `org.jetbrains.kotlin.android` не подключён и не нужен; подключены только `kotlin-compose` и `kotlin-serialization`.
- DI нет: `ApiClient` — object, `CheapEatsRepository` создаётся на месте в ViewModel.

```bash
./gradlew :app:assembleDebug              # APK: app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:testDebugUnitTest          # единственный тест — заглушка ExampleUnitTest
./gradlew :app:connectedDebugAndroidTest  # требует запущенный эмулятор
```

Своих тестов, ktlint и CI нет: реальная проверка — сборка + ручной прогон. Один тест: `./gradlew :app:testDebugUnitTest --tests "com.kovaleff.cheapeats.ExampleUnitTest"`.

## Установка и эмулятор

```bash
ADB=/mnt/dockerDisk/Progs/Android/Sdk/platform-tools/adb
$ADB install -r app/build/outputs/apk/debug/app-debug.apk
$ADB shell am start -n com.kovaleff.cheapeats/.MainActivity
$ADB shell pm grant com.kovaleff.cheapeats android.permission.ACCESS_FINE_LOCATION
$ADB shell pm grant com.kovaleff.cheapeats android.permission.ACCESS_COARSE_LOCATION
$ADB emu geo fix 38.9898 45.0197   # <lon> <lat>, Краснодар
```

- SDK: `/mnt/dockerDisk/Progs/Android/Sdk` (`local.properties`). AVD `medium_phone` (API 35, x86_64), данные — `/mnt/oldWin/AVD/medium_phone.avd` (`~/.android/avd/medium_phone.ini`). Запуск: `/mnt/dockerDisk/Progs/Android/Sdk/emulator/emulator -avd medium_phone -no-window -gpu swiftshader_indirect`.
- Тапы для UI-проверок — `$ADB shell input tap X Y`, где X/Y в физических пикселях экрана (`medium_phone` — 1080×2400), а не в координатах масштабированного скриншота.
- Держать Gradle/AVD/кэши на дисках с местом: в `/home` его мало.

## Структура

```
MainActivity.kt              NavHost: home / map?categoryId={id}
CheapEatsApplication.kt      Coil ImageLoader поверх общего OkHttp
data/api/                    Retrofit-интерфейсы + ApiConfig (UA, внешние URL)
data/model/                  Models.kt — DTO; ApiDtos.kt — обёртки/запросы
data/remote/ApiClient.kt     OkHttp/Retrofit, devDns, Json
data/repository/             вся бизнес-логика (Overpass, sync, geocode)
ui/home, ui/map, ui/theme
```

- Сетевые вызовы — только из `viewModelScope.launch` через `CheapEatsRepository`; UI не ходит в API напрямую. Состояние — `StateFlow<UiState>`.
- Карта не использует `/venues`: маркеры строятся из живого Overpass-запроса (радиус 2 км) → `POST /venues/sync-overpass` (чанки по 200) → склейка по `osm_id`. С главной из бэкенда берутся только `/cities`, `/categories`, `/reviews`.
- В `CheapEatsApiService` уже объявлены, но пока не используются `/venues`, `/venues/nearby`, `/collections`, comments, `/user`. Новую функциональность вести через репозиторий.
- Авторизации нет: не хранится токен и не добавляется `Authorization: Bearer`, поэтому защищённые методы из спецификации (создание отзыва/фото, `/user`) дадут 401.

## Сеть и dev-сервер

- Base URL задаётся не в `ApiConfig`, а в `app/build.gradle.kts`: `APP_BASE_URL` / `API_BASE_URL` (у API обязателен завершающий `/`). `ApiConfig` только читает `BuildConfig`.
- Имя `cheap-eats` есть только в `/etc/hosts` хоста и из эмулятора не резолвится. `devDns` в `ApiClient` возвращает LAN-адрес (`DEV_HOST_LAN_IP` в `app/build.gradle.kts`, сейчас `192.168.0.102`) и `10.0.2.2`; OkHttp перебирает оба. При смене сети обновить IP (`ip -4 addr show`). SNI и проверка сертификата остаются на исходном имени.
- TLS: сертификат mkcert. Корневой CA — `app/src/main/res/raw/mkcert_ca.pem` + `res/xml/network_security_config.xml` (домен `cheap-eats`). При перевыпуске сертификата обновить PEM.
- OkHttp/Android **не отправляет SNI для односоставного хоста** `cheap-eats`, поэтому nginx отдаёт default-сертификат (`localhost.pem`). В его SAN обязан входить `cheap-eats`, иначе `SSLPeerUnverifiedException: Hostname cheap-eats not verified`. Серверные файлы: `/home/kovaleff/www/cheap-eats/{cheap-eats,localhost}.pem`, конфиги — `/etc/nginx/sites-available/cheap-eats*.conf`; перевыпуск — `mkcert -cert-file localhost.pem -key-file localhost-key.pem localhost 127.0.0.1 ::1 cheap-eats`, затем `sudo nginx -t && sudo systemctl reload nginx`.
- Coil обязан использовать `ApiClient.okHttpClientForImages`, иначе картинки с dev-домена не загрузятся (`CheapEatsApplication`).
- `Json` настроен на `ignoreUnknownKeys/isLenient/coerceInputValues/explicitNulls=false`: DTO могут быть частичными. Не добавлять Moshi/Gson.

## Данные и Overpass

- **`is_default` у City — boolean** (cast в `App\Models\City`), в `CityDto` — `Boolean`. Не возвращать `Int` и сравнение с `1`: разбор `/cities` упадёт. Поля `code` в API больше нет (в DTO осталось опциональным — безвредно).
- `average_check` — строка decimal (`"250.00"`). `osm_id` в путях API — строка, в `SyncOverpass*` DTO — `String`, в `VenueDto` — `Long`. `open_night`, `exists`, `poison_risk` — boolean.
- `exists: false` — заглушка из OSM, в UI помечается «Не проверено».
- `GET .../comments` — по `slug`, `POST .../comments` — по `osm_id` (разные ключи).
- Города и категории кэшируются на бэке на сутки — правки в админке видны не сразу.
- Overpass: не объединять `amenity` в regex с `|` — зеркала за Apache отвечают 500; только отдельные statements (см. `fetchOverpassElements`). Ретраи: 2 круга × 3 зеркала с паузами; 500/502 — временные.
- `sync-overpass` пишет в MySQL на хосте: при `SQLSTATE[HY000] [2002] Connection refused` — `sudo systemctl start mysql` (в этом окружении MySQL часто не запущен).
- Лимиты API: `sync-overpass` 30/мин, comments 10/мин, geocode 20/мин, VK login 10/мин → 429; не ретраить вслепую.
- Пины osmdroid: иконка 96 px, hitTest идёт по битмапу — тапить точно по кружку; размер меняется в `makePinDrawable`.

## Конвенции

- Комментарии — по-русски, про ограничения и причины, а не пересказ кода; полезны ссылки на веб-аналог (`resources/js/services/overpass.js`, `public/css/site.css`).
- Цвета темы в `ui/theme/Color.kt` повторяют `public/css/site.css`.
- Новый эндпоинт: метод в `data/api/CheapEatsApiService.kt`, DTO в `data/model/`, логика — только в `CheapEatsRepository`.
- `classes.jar` и `org/` в корне — распакованный osmdroid (артефакт исследования), в сборке не участвуют; не добавлять в sourceSets.
