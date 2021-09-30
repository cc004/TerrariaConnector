package com.example.trconnector.ui.login;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.trconnector.NetManager;
import com.example.trconnector.R;
import com.example.trconnector.databinding.ActivityLoginBinding;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private MutableLiveData<Boolean> connected = new MutableLiveData<>();

    // A placeholder password validation check
    private boolean isPasswordValid(String password) {
        try{
            int port = Integer.parseInt(password);
            if (port > 0 && port < 65536) return true;
        } catch (Throwable e){

        }
        return false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        final EditText usernameEditText = binding.username;
        final EditText passwordEditText = binding.password;
        final Button loginButton = binding.login;
        final ProgressBar loadingProgressBar = binding.loading;

        connected.observe(this, b -> {
            loginButton.setEnabled(false);
            usernameEditText.setEnabled(false);
            passwordEditText.setEnabled(false);
            loadingProgressBar.setVisibility(View.GONE);
            Toast.makeText(getApplicationContext(), "connected!", Toast.LENGTH_LONG).show();
        });
        TextWatcher afterTextChangedListener = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // ignore
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // ignore
            }

            @Override
            public void afterTextChanged(Editable s) {
                String hostname = usernameEditText.getText().toString();
                String port = passwordEditText.getText().toString();
                if (hostname.isEmpty() || port.isEmpty()) return;

                if (!isPasswordValid(port)) {
                    loginButton.setEnabled(false);
                    passwordEditText.setError(getString(R.string.invalid_password));
                } else {
                    loginButton.setEnabled(true);
                }
            }
        };
        usernameEditText.addTextChangedListener(afterTextChangedListener);
        passwordEditText.addTextChangedListener(afterTextChangedListener);
        passwordEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                loginButton.callOnClick();
            }
            return false;
        });

        loginButton.setOnClickListener(v -> {
            loadingProgressBar.setVisibility(View.VISIBLE);
            String hostname = usernameEditText.getText().toString();
            String port = passwordEditText.getText().toString();
            if (mgr != null) mgr.stop();
            mgr = new NetManager();
            mgr.configure(hostname, port, 7777, () -> {
                mgr.start(() -> connected.postValue(true));
            });
        });
    }

    private NetManager mgr;
}