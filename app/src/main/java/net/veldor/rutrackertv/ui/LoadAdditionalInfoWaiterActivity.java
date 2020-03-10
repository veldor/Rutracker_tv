package net.veldor.rutrackertv.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import net.veldor.rutrackertv.App;
import net.veldor.rutrackertv.LeanbackActivity;
import net.veldor.rutrackertv.R;
import net.veldor.rutrackertv.selections.ExtendedDistributionInfo;

public class LoadAdditionalInfoWaiterActivity extends LeanbackActivity {
    private android.widget.TextSwitcher TextSwitcher;
    private Observer<ExtendedDistributionInfo> FullInfoObserver;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupUI();

        setupObservers();
    }

    private void setupObservers() {

        final LiveData<ExtendedDistributionInfo> extendedInfoContainer = App.getInstance().ExtendedDistributionInfo;

        FullInfoObserver = new Observer<ExtendedDistributionInfo>() {
            @Override
            public void onChanged(ExtendedDistributionInfo extendedDistributionInfo) {
                if(extendedDistributionInfo != null){
                        Log.d("surprise", "TorrentDetailsFragment onChanged: have extended info of this element");
                        App.getInstance().ExtendedDistributionInfo.removeObserver(FullInfoObserver);
                        done(extendedDistributionInfo);
                }
            }
        };

        extendedInfoContainer.observeForever(FullInfoObserver);
    }

    private void setupUI() {
        setContentView(R.layout.wait);
        TextSwitcher = findViewById(R.id.waitingStatusWrapper);
        TextSwitcher.setInAnimation(this, android.R.anim.slide_in_left);
        TextSwitcher.setOutAnimation(this, android.R.anim.slide_out_right);
        setWaitingText(getString(R.string.loading_additional_info_message));
    }

    private void done(final ExtendedDistributionInfo extendedDistributionInfo) {
        setWaitingText(getString(R.string.done_message));
        // через 1 секунду закрою активити
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent();
                intent.putExtra(ExtendedDistributionInfo.ADDITIONAL_DATA, extendedDistributionInfo);
                setResult(RESULT_OK, intent);
                finish();
            }
        }, 1000);
    }

    private void setWaitingText(String status) {
        TextSwitcher.setText(status);
    }
}
