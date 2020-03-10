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

public class WaiterActivity extends LeanbackActivity {

    private android.widget.TextSwitcher TextSwitcher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupUI();

        setupObservers();
    }

    private void setupObservers() {
        LiveData<WorkInfo> startTorWorkStatus = App.getInstance().TorStartWork;

        if(startTorWorkStatus != null){
            startTorWorkStatus.observe(this, new Observer<WorkInfo>() {
                @Override
                public void onChanged(@Nullable WorkInfo workInfo) {
                    // стартую загрузку TOR, жду, пока загрузится
                    if (workInfo != null) {
                        if (workInfo.getState() == SUCCEEDED) {
                            setWaitingText(getString(R.string.done_message));
                            App.getInstance().TorStartWork = null;
                            done();
                        } else if (workInfo.getState() == FAILED) {
                            setWaitingText(getString(R.string.failed_message));
                            done();
                        } else if (workInfo.getState() == RUNNING) {
                            setWaitingText(WaiterActivity.this.getString(R.string.wait_for_initialize));
                        } else if (workInfo.getState() == ENQUEUED) {
                            setWaitingText(WaiterActivity.this.getString(R.string.wait_for_internet_message));
                        }
                    }
                }
            });
        }
        else{
            // считаю, что дело сделано
            Intent intent = new Intent();
            setResult(RESULT_OK, intent);
            finish();
        }

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
