package ru.practice.users;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

public class SignActivity extends Activity {
    protected EditText LoginTextView, PasswordTextView;
    protected Button signInButton;
    private TextView messageTextView;
    private ProgressBar progress;
    protected boolean requestInProgress;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable timeout = () -> {
        finishRequest();
        showError(R.string.connection_error);
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign);
        Init();
    }

    protected void Init() {
        LoginTextView = findViewById(R.id.LoginTextView);
        PasswordTextView = findViewById(R.id.PasswordTextView);
        signInButton = findViewById(R.id.SignInButton);
        messageTextView = findViewById(R.id.MessageTextView);
        progress = findViewById(R.id.SignProgress);
    }

    protected String getLogin() { return LoginTextView.getText().toString().trim(); }
    protected String getPassword() { return PasswordTextView.getText().toString(); }

    public void SignIn(View view) {
        if (requestInProgress) return;
        boolean loginValid = Transform.StringNotNull(getLogin());
        boolean passwordValid = Transform.StringNotNull(getPassword());
        if (!(loginValid & passwordValid)) {
            Transform.Vibrate(this);
            showError(R.string.NullParametersMessage);
            return;
        }
        if (!Transform.isValidLogin(getLogin())) {
            showError(R.string.invalid_login);
            return;
        }
        final String password = getPassword();
        beginRequest();
        DatabaseReference account = FirebaseConnection.getDatabase(this)
                .getReference(UserStaticInfo.USERS_SIGN_IN_INFO).child(getLogin());
        account.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(DataSnapshot snapshot) {
                if (!requestInProgress || isFinishing() || isDestroyed()) return;
                finishRequest();
                Object storedPassword = snapshot.child(UserStaticInfo.PASSWORD).getValue();
                Object profile = snapshot.child(UserStaticInfo.PROFILE_ID).getValue();
                if (storedPassword != null && profile != null
                        && password.equals(storedPassword.toString())) {
                    goNext(profile.toString());
                } else {
                    CantSignIn();
                }
            }
            @Override public void onCancelled(DatabaseError error) {
                if (isFinishing() || isDestroyed()) return;
                finishRequest();
                showError(R.string.connection_error);
            }
        });
    }

    protected void beginRequest() {
        requestInProgress = true;
        signInButton.setEnabled(false);
        progress.setVisibility(View.VISIBLE);
        messageTextView.setVisibility(View.GONE);
        handler.postDelayed(timeout, 12000);
    }
    protected void finishRequest() {
        requestInProgress = false;
        handler.removeCallbacks(timeout);
        signInButton.setEnabled(true);
        progress.setVisibility(View.GONE);
    }
    protected void showError(int message) {
        messageTextView.setText(message);
        messageTextView.setVisibility(View.VISIBLE);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    protected void CantSignIn() { showError(R.string.CantSignInMessage); }

    protected void goNext(String profileId) {
        UserStaticInfo.profileId = profileId;
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
