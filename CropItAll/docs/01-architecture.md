# Архитектура CropItAll

## Назначение
Приложение для массовой обрезки однотипных скриншотов по заданному шаблону.
Основная задача — освободить память телефона и навести порядок в сотнях скриншотов.

## Технологии
- **Язык:** Kotlin, последняя стабильная версия.
- **Min SDK:** API 24 (Android 7.0). **Target SDK:** API 36.
- **Пакет:** `com.mahksim.CropItAll`
- **UI:** XML-разметка + ViewBinding (без Jetpack Compose).
- **Архитектура:** MVVM (ViewModel + StateFlow).
- **Хранение настроек:** DataStore Preferences.
- **Работа с файлами:** Storage Access Framework (SAF).
- **Работа с изображениями:** `BitmapFactory`, `Bitmap.createBitmap`, `Bitmap.compress`.
- **Асинхронность:** Kotlin Coroutines (`Dispatchers.IO`).
- **Навигация:** ViewPager2 внутри `CropActivity`.

## Структура пакетов

com.mahksim.CropItAll/
├── data/
│   ├── FileRepository.kt          (интерфейс)
│   ├── FileRepositoryImpl.kt      (реализация через SAF)
│   ├── SettingsRepository.kt      (интерфейс)
│   └── SettingsRepositoryImpl.kt  (реализация через DataStore)
├── ui/
│   ├── main/
│   │   ├── MainActivity.kt
│   │   └── MainViewModel.kt
│   └── crop/
│       ├── CropActivity.kt
│       ├── CropViewModel.kt
│       ├── CropOverlayView.kt
│       └── ImagePagerAdapter.kt
└── util/
├── BitmapUtils.kt             (обрезка, сжатие)
└── FileUtils.kt               (имена файлов, размеры)


## Принципы
- **Без DI-библиотек.** Репозитории создаются вручную в Activity.
- **Интерфейсы + реализации** — для гибкости и тестируемости.
- **Все опасные операции** (чтение, запись, удаление) — в `try-catch`.
- **Все файловые операции** — в `Dispatchers.IO`.
- **Только портретная ориентация** для `CropActivity`.

## Компоненты

### MainActivity
- Выбор исходной папки (`ACTION_OPEN_DOCUMENT_TREE`).
- Ввод названия вебинара.
- Выбор режима: удалять исходники или перемещать в Корзину.
- Кнопка «FRAME» → переход в `CropActivity`.
- Информационное поле И1 (шаги) и И2 (журнал последней операции).
- Проверка «Папка не пуста» до входа в `CropActivity`.

### CropActivity
- ViewPager2 для листания изображений.
- CropOverlayView — рамка выделения.
- Верхняя панель: кнопка «Crop it ALL».
- Нижняя панель: кнопки «ГОТОВО» и «Crop it».
- Ряд навигации: `<<`, `<`, `>`, `>>`.

### CropOverlayView (кастомный View, с нуля)
- Рисует затемнение (50% чёрного) и рамку.
- Минимальный размер рамки: 70×70 dp.
- Хватательные зоны: адаптивные, 10% от размера рамки, min 24 dp, max 40 dp.
- Углы и стороны не перекрываются.
- Приоритет захвата: углы → стороны → центр.
- Предотвращение «переворота» рамки (остановка на minSize).
- Не выходит за границы View.

## Ограничения
- Приложение не требует интернета.
- Никакой рекламы, аналитики, сторонних SDK.
- Работает только с изображениями, доступными через SAF.
- Разрешения `READ_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE` не используются.