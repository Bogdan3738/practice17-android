package ru.practice.users;

import android.content.Context;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.FirebaseDatabase;

/** Официальный Firebase SDK подключается к выбранному учебному Emulator. */
public final class FirebaseConnection {
    private static FirebaseDatabase database;
    private FirebaseConnection() { }

    public static synchronized FirebaseDatabase getDatabase(Context context) {
        if (database == null) {
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setProjectId("demo-practice17")
                    .setApplicationId("1:1234567890:android:1234567890abcdef123456")
                    .setApiKey("AIzaSyDemoPractice17EmulatorOnly000000000")
                    .setDatabaseUrl("https://demo-practice17-default-rtdb.firebaseio.com")
                    .build();
            FirebaseApp app = FirebaseApp.initializeApp(context.getApplicationContext(), options, "Practice17");
            database = FirebaseDatabase.getInstance(app);
            database.useEmulator(context.getString(R.string.firebase_emulator_host), 9000);
        }
        return database;
    }
}
