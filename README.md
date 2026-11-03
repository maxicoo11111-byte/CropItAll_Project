# CropItAll

Приложение для массовой обрезки скриншотов. 
Проект включает в себя две версии: для мобильных устройств на Android и для компьютеров на Windows.

## 📱 Android-версия
- Язык: Kotlin
- Технологии: MVVM, Storage Access Framework (SAF), DataStore, ViewPager2.
- Особенности: кастомная рамка обрезки, сохранение в приватную папку, режим «Корзина».

## 💻 Windows-версия
- Язык: Python 3.10+
- Технологии: CustomTkinter, Pillow.
- Особенности: удобный десктопный интерфейс, пакетная обработка, подсчет сэкономленной памяти.

## 🚀 Как запустить
- **Android:** Откройте папку `CropItAll` в Android Studio и запустите на устройстве.
- **Windows:** Перейдите в папку `CropItAll_PC`, установите зависимости (`pip install customtkinter pillow`) и запустите `python main.py`.