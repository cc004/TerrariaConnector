package com.example.trconnector.ui.login;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import android.util.Patterns;

import com.example.trconnector.R;

public class LoginViewModel extends ViewModel {

    private MutableLiveData<LoginFormState> loginFormState = new MutableLiveData<>();
    private MutableLiveData<LoginResult> loginResult = new MutableLiveData<>();

    LoginViewModel() {
    }

    LiveData<LoginFormState> getLoginFormState() {
        return loginFormState;
    }

    LiveData<LoginResult> getLoginResult() {
        return loginResult;
    }

    public void login() {
        loginResult.postValue(new LoginResult(new LoggedInUserView("connected")));
    }

    public void loginDataChanged(String username, String password) {
        if (username.isEmpty() || password.isEmpty()) return;

        if (!isPasswordValid(password)) {
            loginFormState.setValue(new LoginFormState(R.string.invalid_password));
        } else {
            loginFormState.setValue(new LoginFormState(true));
        }
    }

    // A placeholder password validation check
    private boolean isPasswordValid(String password) {
        for (int i = 0; i < password.length(); ++i)
        {
            char c = password.charAt(i);
            if (c < '0' || c > '9') return false;
        }
        return true;
    }
}