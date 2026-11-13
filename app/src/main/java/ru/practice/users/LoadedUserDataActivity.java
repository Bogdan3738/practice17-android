package ru.practice.users;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class LoadedUserDataActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private DatabaseReference reference;
    private ValueEventListener listener;
    private List<User> pendingUsers;
    private long startedAt;
    private boolean resumed;
    private boolean loading;
    private final Runnable timeout = () -> showFailure(R.string.profile_load_failed);
    private final Runnable enterMain = () -> {
        if (!resumed || pendingUsers == null || isFinishing()) return;
        UserStaticInfo.users.clear();
        UserStaticInfo.users.addAll(pendingUsers);
        pendingUsers = null;
        startActivity(new Intent(this, MainActivity.class));
        finish();
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loaded_user_data);
        Init();
        loadProfiles();
    }
    private void Init() {
        findViewById(R.id.imageForLoaded).startAnimation(
                AnimationUtils.loadAnimation(this, R.anim.logo_rotate_animation));
    }
    private void loadProfiles() {
        cancelRead();
        pendingUsers = null;
        loading = true;
        startedAt = SystemClock.uptimeMillis();
        ((TextView) findViewById(R.id.LoadingStatus)).setText(R.string.loading_users);
        findViewById(R.id.RetryButton).setVisibility(View.GONE);
        findViewById(R.id.LoadingBackButton).setVisibility(View.GONE);
        if (!Transform.StringNotNull(UserStaticInfo.profileId)) {
            Back(null);return;
        }
        handler.postDelayed(timeout, 15000);
        reference = FirebaseConnection.getDatabase(this).getReference(UserStaticInfo.USERS_PROFILE_INFO);
        listener = new ValueEventListener() {
            @Override public void onDataChange(DataSnapshot snapshot) {
                if (!loading || isFinishing() || isDestroyed()) return;
                List<User> result = new ArrayList<>();
                User ownUser = null;
                for (DataSnapshot profile : snapshot.getChildren()) {
                    String name = value(profile, UserStaticInfo.NAME);
                    String state = value(profile, UserStaticInfo.STATE);
                    int age = Transform.parseIntOrDefault(value(profile, UserStaticInfo.AGE), 0);
                    boolean own = UserStaticInfo.profileId.equals(profile.getKey());
                    User user = new User(name, state, age, own ? 1 : 0);
                    if (own) ownUser = user;
                    else result.add(user);
                }
                if (ownUser == null) { showFailure(R.string.profile_missing);return; }
                result.add(0, ownUser);
                pendingUsers = result;
                loading = false;
                handler.removeCallbacks(timeout);
                ((TextView) findViewById(R.id.LoadingStatus)).setText(R.string.profile_ready);
                scheduleEnter();
            }
            @Override public void onCancelled(DatabaseError error) {
                if (!isFinishing() && !isDestroyed()) showFailure(R.string.profile_load_failed);
            }
        };
        reference.addListenerForSingleValueEvent(listener);
    }
    private String value(DataSnapshot snapshot, String key) {
        Object data = snapshot.child(key).getValue();
        return data == null ? "" : data.toString();
    }
    private void scheduleEnter() {
        handler.removeCallbacks(enterMain);
        if (resumed && pendingUsers != null) {
            long remaining = Math.max(0, 3500 - (SystemClock.uptimeMillis() - startedAt));
            handler.postDelayed(enterMain, remaining);
        }
    }
    private void showFailure(int message) {
        loading = false;
        handler.removeCallbacks(timeout);
        ((TextView) findViewById(R.id.LoadingStatus)).setText(message);
        findViewById(R.id.RetryButton).setVisibility(View.VISIBLE);
        findViewById(R.id.LoadingBackButton).setVisibility(View.VISIBLE);
    }
    private void cancelRead() {
        handler.removeCallbacks(timeout);
        handler.removeCallbacks(enterMain);
        if (reference != null && listener != null) reference.removeEventListener(listener);
    }
    public void Retry(View view) { loadProfiles(); }
    public void Back(View view) {
        getSharedPreferences(Transform.APP_PREFERENCES, MODE_PRIVATE).edit().clear().apply();
        UserStaticInfo.profileId = null;
        startActivity(new Intent(this, SignActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }
    @Override protected void onResume() {
        super.onResume();resumed = true;scheduleEnter();
    }
    @Override protected void onPause() {
        resumed = false;handler.removeCallbacks(enterMain);super.onPause();
    }
    @Override protected void onDestroy() {
        cancelRead();handler.removeCallbacksAndMessages(null);super.onDestroy();
    }
    @Override public void onBackPressed() { Back(null); }
}
