# CheckersAndroid (Шашки)

Android-застосунок класичної гри **«Шашки»** (людина проти комп'ютера), розроблений мовою **Kotlin** із власною графічною дошкою, алгоритмом ШІ, конфігурованим логуванням та динамічною локалізацією.

## Основні можливості

* **Режим гри:** людина (білі шашки) проти комп'ютера (чорні шашки) на дошці $8 \times 8$.
* **Класичні правила:**
  * обов'язкове побиття (у тому числі назад для простих шашок);
  * підтримка серійних взяттів (мультиход);
  * перетворення у дамку на крайній горизонталі з ходами й биттям на всю діагональ;
  * автоматичне визначення перемоги, поразки та нічиєї (правило 40 тихих ходів).
* **Комп'ютерний гравець (ШІ):** алгоритм **Minimax з альфа-бета відсіканням** (глибина пошуку — 4 півходи) та евристичною оцінкою позиції (матеріал, дамки, просування вперед, контроль центру, захист тилової лінії). Обчислення виконуються у фоновому потоці.
* **Кастомний інтерфейс:** `CheckersBoardView` на базі Android `Canvas` із підсвіткою останнього ходу, обраної шашки, дозволених ходів/побиттів та корон на дамках.
* **Мультимовність (i18n):** підтримка української (`uk`) та англійської (`en`) мов із миттєвим перемиканням кнопкою `UA / EN` без втрати поточної партії.
* **Логування:** налаштований логер на базі **Timber** із розділенням поведінки для `Debug` та `Release` збірок.

## Стек технологій

* **Мова:** Kotlin
* **Платформа:** Android SDK (`minSdk 24`, `compileSdk 35`, `targetSdk 35`)
* **Збірка:** Gradle 9.2.2, Android Gradle Plugin 9.0.1
* **Бібліотеки:**
  * `androidx.appcompat:appcompat:1.7.0` (підтримка per-app локалізації)
  * `androidx.core:core-ktx:1.13.1`
  * `com.google.android.material:material:1.12.0`
  * `com.jakewharton.timber:timber:5.0.1` (логування)

## Структура проєкту

```text
CheckersAndroid/
├── .gitignore
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── app/
    ├── build.gradle.kts
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/example/checkers/
        │   ├── CheckersApp.kt           # Ініціалізація Timber Logger та збереження стану рушія
        │   ├── MainActivity.kt          # Головний екран, взаємодія з ШІ та перемикання мов
        │   ├── game/
        │   │   ├── CheckersEngine.kt    # Правила гри, валідація ходів, серійні взяття, дамки
        │   │   └── CheckersAI.kt        # ШІ на базі Minimax з альфа-бета відсіканням
        │   └── ui/
        │       └── CheckersBoardView.kt # Відмальовка дошки та обробка натискань
        └── res/
            ├── layout/activity_main.xml # Розмітка головного екрана
            ├── values/strings.xml       # Англійська локалізація (за замовчуванням)
            ├── values-uk/strings.xml    # Українська локалізація
            └── xml/locales_config.xml   # Конфігурація підтримуваних мов (Android 13+)
```

## Логування та локалізація

### Logger (`Timber`)
Конфігурація виконується у класі `CheckersApp`:
* **Debug-збірка (`CheckersDebugTree`):** виводить докладні логи всіх подій гри (старт застосунку, ходи, побиття, перетворення в дамку, час роздумів ШІ, перемикання мови) з тегом формату `Checkers/<Клас>:<Рядок>#<Метод>`.
* **Release-збірка (`ReleaseTree`):** відфільтровує `VERBOSE` і `DEBUG` повідомлення, залишаючи лише попередження та помилки (`WARN`, `ERROR`).

### Інтернаціоналізація (i18n)
* Усі текстові рядки інтерфейсу винесені у ресурси `values/strings.xml` та `values-uk/strings.xml` (з підтримкою форматованих рядків для лічильника шашок).
* Перемикання мови під час роботи виконується через `AppCompatDelegate.setApplicationLocales(...)`.
* Для збереження обраної мови на Android 13+ використовується `locales_config.xml`, а на Android 12 і нижче — `AppLocalesMetadataHolderService`.

## Збірка та запуск

### Передумови
* JDK 17+
* Gradle 9.x
* Android SDK (Android API 35, `platform-tools`) та налаштована системна змінна `ANDROID_HOME` (або файл `local.properties` зі шляхом `sdk.dir=...`).

### Команди збірки та запуску (PowerShell / термінал)

1. **Зібрати Debug APK:**
   ```powershell
   gradle assembleDebug
   ```
   Готовий файл буде збережено за шляхом `app/build/outputs/apk/debug/app-debug.apk`.

2. **Встановити та запустити на підключеному Android-пристрої або емуляторі:**
   ```powershell
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   adb shell am start -n com.example.checkers/.MainActivity
   ```

3. **Переглянути логи роботи застосунку в реальному часі:**
   ```powershell
   adb logcat | Select-String "Checkers/"
   ```