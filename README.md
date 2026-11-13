# Практика 17 — Java + XML

Приложение продолжает практику 16. Для учебной базы используется официальный Firebase Realtime Database Emulator.

Android Studio: JDK 17, SDK 35, Build Tools 34.0.0. Сборка: `./gradlew assembleDebug`.

Firebase Emulator должен работать на порту 9000 компьютера, где запущен Android Emulator. Адрес в приложении — `10.0.2.2`. Это локальная учебная конфигурация без подключения к облачной базе.

Запуск учебной базы после установки Firebase CLI: `firebase emulators:start --only database --project demo-practice17`. Правила предназначены для локального Emulator. Новая учётная запись создаётся во вкладке «Регистрация».
