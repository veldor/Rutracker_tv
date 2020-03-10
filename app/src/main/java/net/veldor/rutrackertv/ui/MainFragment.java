/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package net.veldor.rutrackertv.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.leanback.app.BackgroundManager;
import androidx.leanback.app.BrowseSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.OnItemViewSelectedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;

import net.veldor.rutrackertv.App;
import net.veldor.rutrackertv.CardPresenter;
import net.veldor.rutrackertv.R;
import net.veldor.rutrackertv.TorrentList;
import net.veldor.rutrackertv.selections.Distribution;
import net.veldor.rutrackertv.viewmodel.MyViewModel;
import net.veldor.rutrackertv.workers.ListInfoWorker;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import static net.veldor.rutrackertv.workers.ListInfoWorker.MASS_PAGE_DETAILS_ACTION;

public class MainFragment extends BrowseSupportFragment {

    private static final int BACKGROUND_UPDATE_DELAY = 300;
    private static final int GRID_ITEM_WIDTH = 500;
    private static final int GRID_ITEM_HEIGHT = 300;
    private static final int NUM_ROWS = 1;
    private final Handler mHandler = new Handler();
    private Drawable mDefaultBackground;
    private DisplayMetrics mMetrics;
    private Timer mBackgroundTimer;
    private String mBackgroundUri;
    private BackgroundManager mBackgroundManager;
    private Activity ParentActivity;
    private MyViewModel ViewModel;
    private ArrayObjectAdapter mRowsAdapter;
    private ArrayObjectAdapter mDistributionListRowAdapter;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ViewModel = new ViewModelProvider(this).get(MyViewModel.class);

        if (getActivity() != null)
            ParentActivity = getActivity();
        // буду отслеживать результаты поиска
        lookForSearchResult();

        // подготовка класса, отвечающего за фоновое изображение для приложения
        prepareBackgroundManager();

        // кастомизация элементов интерфейса
        setupUIElements();

        // загрузка контента
        loadRows();

        setupEventListeners();
    }

    private void lookForSearchResult() {
        final LiveData<ArrayList<Distribution>> distributionStorage = App.getInstance().SearchedDistributions;
        Activity activity = getActivity();
        if (activity != null) {
            distributionStorage.observe((LifecycleOwner) activity, new Observer<ArrayList<Distribution>>() {
                @Override
                public void onChanged(ArrayList<Distribution> distributions) {
                    if (distributions != null) {
                        if (distributions.size() > 0) {
                            Toast.makeText(getContext(), getString(R.string.search_result_loaded_message), Toast.LENGTH_SHORT).show();
                            loadRows(distributions);
                            distributionStorage.removeObservers(MainFragment.this);
                        } else {
                            Toast.makeText(getContext(), getString(R.string.search_empty_message), Toast.LENGTH_LONG).show();
                            loadRows();
                            distributionStorage.removeObservers(MainFragment.this);
                        }
                    }
                }
            });
        }
    }

    private void loadRows(ArrayList<Distribution> distributions) {
        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        CardPresenter cardPresenter = new CardPresenter();

        mDistributionListRowAdapter = new ArrayObjectAdapter(cardPresenter);
        if (distributions != null && distributions.size() > 0) {
            for (Distribution distribution : distributions) {
                mDistributionListRowAdapter.add(distribution);
            }
        }
        HeaderItem header = new HeaderItem(0, TorrentList.TORRENT_CATEGORY[0]);
        mRowsAdapter.add(new ListRow(header, mDistributionListRowAdapter));

        HeaderItem gridHeader = new HeaderItem(1, getString(R.string.preferences_message));

        GridItemPresenter mGridPresenter = new GridItemPresenter();
        ArrayObjectAdapter gridRowAdapter = new ArrayObjectAdapter(mGridPresenter);
        gridRowAdapter.add(getResources().getString(R.string.personal_settings));
        mRowsAdapter.add(new ListRow(gridHeader, gridRowAdapter));
        setAdapter(mRowsAdapter);
        // запущу рабочего, который будет подгружать полную информацию
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        OneTimeWorkRequest pageDetailsWork = new OneTimeWorkRequest.Builder(ListInfoWorker.class).addTag(MASS_PAGE_DETAILS_ACTION).setConstraints(constraints).build();
        WorkManager.getInstance(App.getInstance()).enqueueUniqueWork(MASS_PAGE_DETAILS_ACTION, ExistingWorkPolicy.REPLACE, pageDetailsWork);
        // запущу отслеживание изменения состояния
        observeDetailsLoading();
    }

    private void observeDetailsLoading() {

        MutableLiveData<Integer> detailsLoad = App.getInstance().isDetailsUpdated;
        if(detailsLoad != null){
            detailsLoad.removeObservers(this);
            detailsLoad.observe(this, new Observer<Integer>() {
                @Override
                public void onChanged(Integer counter) {
                    if(counter != null){
                        mDistributionListRowAdapter.notifyArrayItemRangeChanged(counter,1);
                    }
                }
            });
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (null != mBackgroundTimer) {
            mBackgroundTimer.cancel();
        }
    }

    private void loadRows() {

        ArrayObjectAdapter rowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        CardPresenter cardPresenter = new CardPresenter();

        int i;
        for (i = 0; i < NUM_ROWS; i++) {
            ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(cardPresenter);
            HeaderItem header = new HeaderItem(i, TorrentList.TORRENT_CATEGORY[i]);
            rowsAdapter.add(new ListRow(header, listRowAdapter));
        }

        HeaderItem gridHeader = new HeaderItem(i, getString(R.string.preferences_message));

        GridItemPresenter mGridPresenter = new GridItemPresenter();
        ArrayObjectAdapter gridRowAdapter = new ArrayObjectAdapter(mGridPresenter);
        gridRowAdapter.add(getResources().getString(R.string.personal_settings));
        rowsAdapter.add(new ListRow(gridHeader, gridRowAdapter));
        setAdapter(rowsAdapter);
    }

    private void prepareBackgroundManager() {
        mBackgroundManager = BackgroundManager.getInstance(ParentActivity);
        mBackgroundManager.attach(ParentActivity.getWindow());

        mDefaultBackground = ContextCompat.getDrawable(ParentActivity, R.drawable.default_background);
        mMetrics = new DisplayMetrics();
        ParentActivity.getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
    }

    private void setupUIElements() {
        // тут визуальные настройки
        // R.drawable.videos_by_google_banner));
        setTitle(getString(R.string.browse_title)); // Badge, when set, takes precedent
        // over title
        setHeadersState(HEADERS_ENABLED);
        setHeadersTransitionOnBackEnabled(true);

        // set fastLane (or headers) background color
        setBrandColor(ContextCompat.getColor(ParentActivity, R.color.fastlane_background));
        // set search icon color
        setSearchAffordanceColor(ContextCompat.getColor(ParentActivity, R.color.search_opaque));
    }

    private void setupEventListeners() {
        setOnSearchClickedListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                // открою строку поиска
                Intent intent = new Intent(getActivity(), SearchActivity.class);
                startActivity(intent);
            }
        });

        setOnItemViewClickedListener(new ItemViewClickedListener());
        setOnItemViewSelectedListener(new ItemViewSelectedListener());
    }

    private void updateBackground(String uri) {
        Log.d("surprise", "MainFragment updateBackground: load " + uri);
        int width = mMetrics.widthPixels;
        int height = mMetrics.heightPixels;
        Glide.with(getActivity())
                .load(uri)
                .apply(new RequestOptions()
                        .placeholder(R.drawable.clapper)
                        .optionalCenterCrop()
                )
                .into(new SimpleTarget<Drawable>(width, height) {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        mBackgroundManager.setDrawable(resource);
                    }
                });
        mBackgroundTimer.cancel();
    }

    private void startBackgroundTimer() {
        if (null != mBackgroundTimer) {
            mBackgroundTimer.cancel();
        }
        mBackgroundTimer = new Timer();
        mBackgroundTimer.schedule(new UpdateBackgroundTask(), BACKGROUND_UPDATE_DELAY);
    }


    private class UpdateBackgroundTask extends TimerTask {

        @Override
        public void run() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    updateBackground(mBackgroundUri);
                }
            });
        }
    }


    private static class GridItemPresenter extends Presenter {
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent) {
            TextView view = new TextView(parent.getContext());
            view.setLayoutParams(new ViewGroup.LayoutParams(GRID_ITEM_WIDTH, GRID_ITEM_HEIGHT));
            view.setFocusable(true);
            view.setFocusableInTouchMode(true);
            view.setBackgroundColor(
                    ContextCompat.getColor(App.getInstance(), R.color.default_background));
            view.setTextColor(Color.WHITE);
            view.setGravity(Gravity.CENTER);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, Object item) {
            ((TextView) viewHolder.view).setText((String) item);
        }

        @Override
        public void onUnbindViewHolder(ViewHolder viewHolder) {
        }
    }


    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (item instanceof Distribution) {
                Distribution distribution = (Distribution) item;
                // активирую загрузку сведений о странице
                App.getInstance().ExtendedDistributionInfo.postValue(null);
                ViewModel.getPageData(distribution.Href);
                FragmentActivity activity = getActivity();
                if (activity != null) {
                    Intent intent = new Intent(getActivity(), DetailsActivity.class);
                    intent.putExtra(DetailsActivity.DISTRIBUTION, distribution);
                    Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                            activity,
                            ((ImageCardView) itemViewHolder.view).getMainImageView(),
                            DetailsActivity.SHARED_ELEMENT_NAME)
                            .toBundle();
                    activity.startActivity(intent, bundle);
                } else {
                    Log.d("surprise", "ItemViewClickedListener onItemClicked click on something else");
                }
            }
            else{
                Log.d("surprise", "ItemViewClickedListener onItemClicked: " + item);
                Log.d("surprise", "ItemViewClickedListener onItemClicked: " + R.string.personal_settings);
               if(item.equals(getString(R.string.personal_settings))){
                   // запущу активити с настройками
                   startActivity(new Intent(App.getInstance(), SettingsActivity.class));
               }
            }
        }
    }


    private final class ItemViewSelectedListener implements OnItemViewSelectedListener {
        @Override
        public void onItemSelected(
                Presenter.ViewHolder itemViewHolder,
                Object item,
                RowPresenter.ViewHolder rowViewHolder,
                Row row) {
            if (item instanceof Distribution) {
                if(((Distribution) item).ExtendedInfo != null && ((Distribution) item).ExtendedInfo.PostImageUrl != null)
                mBackgroundUri = ((Distribution) item).ExtendedInfo.PostImageUrl;
                startBackgroundTimer();
            }
        }
    }


}
