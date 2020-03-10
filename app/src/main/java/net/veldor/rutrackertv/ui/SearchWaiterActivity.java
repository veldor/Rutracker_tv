package net.veldor.rutrackertv.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.work.WorkInfo;

import net.veldor.rutrackertv.App;
import net.veldor.rutrackertv.LeanbackActivity;
import net.veldor.rutrackertv.R;

import static androidx.work.WorkInfo.State.ENQUEUED;
import static androidx.work.WorkInfo.State.FAILED;
import static androidx.work.WorkInfo.State.RUNNING;
import static androidx.work.WorkInfo.State.SUCCEEDED;

public class SearchWaiterActivity extends LeanbackActivity {
    private android.widget.TextSwitcher TextSwitcher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupUI();

        setupObservers();
    }

    private void setupObservers() {
        final LiveData<WorkInfo> searchWorkStatus = App.getInstance().SearchWorkStatus;
        if (searchWorkStatus == null) {
            // процесс не найден, верну удачный результат
            done();
        } else
            searchWorkStatus.observe(this, new Observer<WorkInfo>() {
                @Override
                public void onChanged(WorkInfo workInfo) {
                    if (workInfo != null) {
                        if (workInfo.getState() == SUCCEEDED) {
                            setWaitingText(getString(R.string.done_message));
                            searchWorkStatus.removeObservers(SearchWaiterActivity.this);
                            done();
                        } else if (workInfo.getState() == FAILED) {
                            setWaitingText(getString(R.string.failed_message));
                            done();
                        } else if (workInfo.getState() == RUNNING) {
                            setWaitingText(getString(R.string.search_in_progress));
                        } else if (workInfo.getState() == ENQUEUED) {
                            setWaitingText(getString(R.string.wait_for_internet_message));
                        }
                    }
                }
            });

        final LiveData<String> requestStatus = App.getInstance().RequestStatus;
        requestStatus.observe(this, new Observer<String>() {
            @Override
            public void onChanged(String s) {
                if(s != null && !s.isEmpty()){
                    setWaitingText(s);
                }
            }
        });
    }

    private void setupUI() {
        setContentView(R.layout.wait);
        TextSwitcher = findViewById(R.id.waitingStatusWrapper);
        TextSwitcher.setInAnimation(this, android.R.anim.slide_in_left);
        TextSwitcher.setOutAnimation(this, android.R.anim.slide_out_right);
    }

    private void done() {
        // через 1 секунду закрою активити
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent();
                setResult(RESULT_OK, intent);
                finish();
            }
        }, 1000);
    }

    private void setWaitingText(String status) {
        TextSwitcher.setText(status);
    }
}
