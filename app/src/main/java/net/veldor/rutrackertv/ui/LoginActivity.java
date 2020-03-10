package net.veldor.rutrackertv.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.work.WorkInfo;

import net.veldor.rutrackertv.App;
import net.veldor.rutrackertv.LeanbackActivity;
import net.veldor.rutrackertv.R;
import net.veldor.rutrackertv.viewmodel.MyViewModel;

import static androidx.work.WorkInfo.State.ENQUEUED;
import static androidx.work.WorkInfo.State.FAILED;
import static androidx.work.WorkInfo.State.RUNNING;
import static androidx.work.WorkInfo.State.SUCCEEDED;

public class LoginActivity extends LeanbackActivity {
    private android.widget.TextSwitcher TextSwitcher;
    private MyViewModel ViewModel;
    private EditText passwordInput;
    private EditText loginInput;
    private Button loginButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupUI();
        setupViewModel();
    }

    private void setupViewModel() {
        // добавлю viewModel
        ViewModel = new ViewModelProvider(this).get(MyViewModel.class);
    }

    private void setupUI() {
        setContentView(R.layout.login);

        loginInput = findViewById(R.id.login_input);
        loginInput.requestFocus();
        passwordInput = findViewById(R.id.password_input);
        loginButton = findViewById(R.id.login_button);
        final ProgressBar progressBar = findViewById(R.id.progress_view);
        TextSwitcher = findViewById(R.id.statusWrapper);
        TextSwitcher.setInAnimation(this, android.R.anim.slide_in_left);
        TextSwitcher.setOutAnimation(this, android.R.anim.slide_out_right);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // проверю наличие логина и пароля
                if(loginInput.getText().toString().isEmpty()){
                    loginInput.requestFocus();
                    Toast.makeText(LoginActivity.this, LoginActivity.this.getString(R.string.empty_login_message), Toast.LENGTH_LONG).show();
                }
                else if(passwordInput.getText().toString().isEmpty()){
                    passwordInput.requestFocus();
                    Toast.makeText(LoginActivity.this, LoginActivity.this.getString(R.string.empty_password_message), Toast.LENGTH_LONG).show();
                }
                else{
                    InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    // отправлю данные
                    loginInput.setEnabled(false);
                    passwordInput.setEnabled(false);
                    loginButton.setEnabled(false);
                    loginButton.setText(R.string.login_in_process_message);
                    progressBar.setVisibility(View.VISIBLE);
                    TextSwitcher.setVisibility(View.VISIBLE);
                    TextSwitcher.setText(LoginActivity.this.getString(R.string.sending_request_message));
                    LiveData<WorkInfo> worker = ViewModel.logMeIn(loginInput.getText().toString(), passwordInput.getText().toString());
                    observeRequest(worker);
                }
            }
        });
    }

    private void observeRequest(final LiveData<WorkInfo> worker) {
        // также подпишусь на обновления статуса запроса
        final LiveData<String> requestStatus = App.getInstance().RequestStatus;
        worker.observe(this, new Observer<WorkInfo>() {
            @Override
            public void onChanged(WorkInfo workInfo) {
                if (workInfo != null) {
                    if (workInfo.getState() == SUCCEEDED) {
                        setStatusText(getString(R.string.done_message));
                        worker.removeObservers(LoginActivity.this);
                        requestStatus.removeObservers(LoginActivity.this);
                        done();
                    } else if (workInfo.getState() == FAILED) {
                        setStatusText(getString(R.string.failed_message));
                        unlockElements();
                        // разблокирую всё
                    } else if (workInfo.getState() == RUNNING) {
                        setStatusText(getString(R.string.wait_for_initialize));
                    } else if (workInfo.getState() == ENQUEUED) {
                        setStatusText(getString(R.string.wait_for_internet_message));
                    }
                }
            }
        });
        requestStatus.observe(this, new Observer<String>() {
            @Override
            public void onChanged(String s) {
                if(s != null && !s.isEmpty()){
                    setStatusText(s);
                }
            }
        });
    }

    private void unlockElements() {
        loginInput.setEnabled(true);
        passwordInput.setEnabled(true);
        loginButton.setEnabled(true);
        loginButton.setText(R.string.login_button_text);
    }

    private void setStatusText(String status) {
        TextSwitcher.setText(status);
    }


    private void done() {
        // через 3 секунды закрою активити
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent();
                setResult(RESULT_OK, intent);
                finish();
            }
        }, 2000);
    }
}
