package ru.practice.users;

/** Безопасное преобразование возраста: ошибочный ввод сохраняет прежнее значение. */
public final class Transform {
    private Transform() { }
    public static int parseIntOrDefault(String whatParse, int defaultValue) {
        try {
            return Integer.parseInt(whatParse == null ? "" : whatParse.trim());
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }
    public static boolean StringNotNull(String value) {
        return value != null && !value.trim().isEmpty();
    }
    public static boolean isValidLogin(String value) {
        return value != null && value.matches("[A-Za-z0-9_\\-]{1,64}");
    }
    public static void Vibrate(android.content.Context context) {
        android.os.Vibrator vibrator = (android.os.Vibrator)
                context.getSystemService(android.content.Context.VIBRATOR_SERVICE);
        if (vibrator == null || !vibrator.hasVibrator()) return;
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(android.os.VibrationEffect.createOneShot(
                    120, android.os.VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(120);
        }
    }
}
