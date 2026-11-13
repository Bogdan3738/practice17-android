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
import android.widget.TabHost;
import java.util.Map;
import java.util.HashMap;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

public class SignActivity extends Activity {
    protected EditText LoginTextView, PasswordTextView;
    protected Button signInButton, signUpButton;
    protected EditText NewLoginTextView, NewPasswordTextView, NewNameTextView, NewAgeTextView, NewStateTextView;
    private TabHost tabHost;
    protected String sessionLogin;
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
        FirebaseConnection.getDatabase(this).getReference();
        CheckSignInInfo();
        if (savedInstanceState != null) tabHost.setCurrentTab(savedInstanceState.getInt("tab", 0));
    }

    protected void Init() {
        LoginTextView = findViewById(R.id.LoginTextView);
        PasswordTextView = findViewById(R.id.PasswordTextView);
        signInButton = findViewById(R.id.SignInButton);
        messageTextView = findViewById(R.id.MessageTextView);
        progress = findViewById(R.id.SignProgress);
        NewLoginTextView = findViewById(R.id.NewLoginTextView);
        NewPasswordTextView = findViewById(R.id.NewPasswordTextView);
        NewNameTextView = findViewById(R.id.NewNameTextView);
        NewAgeTextView = findViewById(R.id.NewAgeTextView);
        NewStateTextView = findViewById(R.id.NewStateTextView);
        signUpButton = findViewById(R.id.SignUpButton);
        tabHost = findViewById(R.id.tabHost);
        tabHost.setup();
        tabHost.addTab(tabHost.newTabSpec("signIn").setIndicator(getString(R.string.signIn)).setContent(R.id.tabSignIn));
        tabHost.addTab(tabHost.newTabSpec("signUp").setIndicator(getString(R.string.signUp)).setContent(R.id.tabSignUp));
    }

    protected String getLogin() { return LoginTextView.getText().toString().trim(); }
    protected String getPassword() { return PasswordTextView.getText().toString(); }

    public void SignIn(View view) {
        if (requestInProgress) return;
        boolean loginValid = Transform.CheckField(LoginTextView);
        boolean passwordValid = Transform.CheckField(PasswordTextView);
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
        sessionLogin = getLogin();
        beginRequest();
        DatabaseReference account = FirebaseConnection.getDatabase(this)
                .getReference(UserStaticInfo.USERS_SIGN_IN_INFO).child(sessionLogin);
        account.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(DataSnapshot snapshot) {
                if (!requestInProgress || isFinishing() || isDestroyed()) return;
                finishRequest();
                Object storedPassword = snapshot.child(UserStaticInfo.PASSWORD).getValue();
                Object profile = snapshot.child(UserStaticInfo.PROFILE_ID).getValue();
                if (storedPassword != null && profile != null
                        && password.equals(storedPassword.toString())) {
                    goNext(profile.toString(), sessionLogin, password);
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
        signUpButton.setEnabled(false);
        progress.setVisibility(View.VISIBLE);
        messageTextView.setVisibility(View.GONE);
        handler.postDelayed(timeout, 30000);
    }
    protected void finishRequest() {
        requestInProgress = false;
        handler.removeCallbacks(timeout);
        signInButton.setEnabled(true);
        signUpButton.setEnabled(true);
        progress.setVisibility(View.GONE);
    }
    protected void showError(int message) {
        messageTextView.setText(message);
        messageTextView.setVisibility(View.VISIBLE);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    protected void CantSignIn() { showError(R.string.CantSignInMessage); }

    protected void CheckSignInInfo() {
        android.content.SharedPreferences preferences = getSharedPreferences(
                Transform.APP_PREFERENCES, MODE_PRIVATE);
        LoginTextView.setText(preferences.getString(UserStaticInfo.LOGIN, ""));
        PasswordTextView.setText(preferences.getString(UserStaticInfo.PASSWORD, ""));
        if (Transform.StringNotNull(getLogin()) && Transform.StringNotNull(getPassword())) {
            SignIn(null);
        }
    }

    protected void goNext(String profileId, String login, String password) {
        Transform.SaveUser(getSharedPreferences(Transform.APP_PREFERENCES, MODE_PRIVATE),
                login, password);
        UserStaticInfo.profileId = profileId;
        startActivity(new Intent(this, LoadedUserDataActivity.class));
        finish();
    }
    protected String getNewLogin() { return NewLoginTextView.getText().toString().trim(); }
    protected String getNewPassword() { return NewPasswordTextView.getText().toString(); }
    protected String getNewName() { return NewNameTextView.getText().toString().trim(); }
    protected String getNewState() { return NewStateTextView.getText().toString(); }
    protected int getNewAge() { return Transform.parseIntOrDefault(NewAgeTextView.getText().toString(), 0); }

    public void SignUp(View view) {
        if (requestInProgress) return;
        boolean filled = Transform.CheckField(NewLoginTextView)
                & Transform.CheckField(NewPasswordTextView)
                & Transform.CheckField(NewNameTextView)
                & Transform.CheckField(NewAgeTextView)
                & Transform.CheckField(NewStateTextView);
        if (!filled) {
            Transform.Vibrate(this);
            showError(R.string.NullParametersMessage);
            return;
        }
        if (!Transform.isValidLogin(getNewLogin())) {
            showError(R.string.invalid_login);
            return;
        }
        sessionLogin = getNewLogin();
        beginRequest();
        createNewUser();
    }

    protected void createNewUser() {
        DatabaseReference root = FirebaseConnection.getDatabase(this).getReference();
        String profileId = root.child(UserStaticInfo.USERS_PROFILE_INFO).push().getKey();
        if (profileId == null) {
            finishRequest();showError(R.string.connection_error);return;
        }
        Map<String, Object> profile = new HashMap<>();
        profile.put(UserStaticInfo.NAME, getNewName());
        profile.put(UserStaticInfo.AGE, getNewAge());
        profile.put(UserStaticInfo.STATE, getNewState());
        Map<String, Object> account = new HashMap<>();
        account.put(UserStaticInfo.PASSWORD, getNewPassword());
        account.put(UserStaticInfo.PROFILE_ID, profileId);
        final String login = getNewLogin();
        root.child(UserStaticInfo.USERS_SIGN_IN_INFO).child(login)
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(DataSnapshot snapshot) {
                if (isFinishing() || isDestroyed()) return;
                if (snapshot.child(UserStaticInfo.PASSWORD).exists()) {
                    finishRequest();showError(R.string.UserExistMessage);return;
                }
                Map<String,Object> updates = new HashMap<>();
                updates.put(UserStaticInfo.USERS_SIGN_IN_INFO + "/" + login, account);
                updates.put(UserStaticInfo.USERS_PROFILE_INFO + "/" + profileId, profile);
                root.updateChildren(updates).addOnCompleteListener(task -> {
                    if (isFinishing() || isDestroyed()) return;
                    finishRequest();
                    if (task.isSuccessful()) goNext(profileId, login, account.get(UserStaticInfo.PASSWORD).toString());
                    else showError(R.string.connection_error);
                });
            }
            @Override public void onCancelled(DatabaseError error) {
                if (isFinishing() || isDestroyed()) return;
                finishRequest();showError(R.string.connection_error);
            }
        });
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        outState.putInt("tab", tabHost.getCurrentTab());
        super.onSaveInstanceState(outState);
    }

    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
