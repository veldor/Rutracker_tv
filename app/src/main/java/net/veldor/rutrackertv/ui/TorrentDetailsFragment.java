/*
 * Copyright (C) 2014 The Android Open Source Project
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
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.leanback.app.DetailsFragment;
import androidx.leanback.app.DetailsFragmentBackgroundController;
import androidx.leanback.widget.Action;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ClassPresenterSelector;
import androidx.leanback.widget.DetailsOverviewRow;
import androidx.leanback.widget.FullWidthDetailsOverviewRowPresenter;
import androidx.leanback.widget.FullWidthDetailsOverviewSharedElementHelper;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.OnActionClickedListener;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import net.veldor.rutrackertv.App;
import net.veldor.rutrackertv.CardPresenter;
import net.veldor.rutrackertv.DetailsDescriptionPresenter;
import net.veldor.rutrackertv.R;
import net.veldor.rutrackertv.Torrent;
import net.veldor.rutrackertv.TorrentList;
import net.veldor.rutrackertv.selections.Distribution;
import net.veldor.rutrackertv.selections.ExtendedDistributionInfo;

import java.util.Collections;
import java.util.List;

/*
 * LeanbackDetailsFragment extends DetailsFragment, a Wrapper fragment for leanback details screens.
 * It shows a detailed view of video and its meta plus related videos.
 */
public class TorrentDetailsFragment extends DetailsFragment {

    private static final int ACTION_DOWNLOAD_TORRENT = 1;
    private static final int ACTION_LOAD_MAGNET = 2;
    private static final int ACTION_SOME_ELSE = 3;

    private static final int DETAIL_THUMB_WIDTH = 274;
    private static final int DETAIL_THUMB_HEIGHT = 274;

    private static final int NUM_COLS = 10;

    private Distribution mSelectedDistribution;

    private ArrayObjectAdapter mAdapter;
    private ClassPresenterSelector mPresenterSelector;

    private DetailsFragmentBackgroundController mDetailsBackground;
    private Observer<ExtendedDistributionInfo> FullInfoObserver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDetailsBackground = new DetailsFragmentBackgroundController(this);

        Activity activity = getActivity();
        if(activity != null){
            // добавлю отслеживание дополнительной загруженной информации
            handleExtendedInfoLoad();
            // рассериализирую информацию
            mSelectedDistribution =
                    (Distribution) activity.getIntent().getSerializableExtra(DetailsActivity.DISTRIBUTION);
            if (mSelectedDistribution != null) {
                // настройка презентера
                mPresenterSelector = new ClassPresenterSelector();
                mAdapter = new ArrayObjectAdapter(mPresenterSelector);
                // настройка деталей
                setupDetailsOverviewRow();
                setupDetailsOverviewRowPresenter();
                //setupRelatedMovieListRow();
                setAdapter(mAdapter);
                initializeBackground(mSelectedDistribution);
                setOnItemViewClickedListener(new ItemViewClickedListener());
            } else {
                Intent intent = new Intent(getActivity(), MainActivity.class);
                startActivity(intent);
            }
        }
        else{
            Intent intent = new Intent(getActivity(), MainActivity.class);
            startActivity(intent);
        }

    }

    private void handleExtendedInfoLoad() {
        final LiveData<ExtendedDistributionInfo> extendedInfoContainer = App.getInstance().ExtendedDistributionInfo;

        FullInfoObserver = new Observer<ExtendedDistributionInfo>() {
            @Override
            public void onChanged(ExtendedDistributionInfo extendedDistributionInfo) {
                if(extendedDistributionInfo != null){
                    // проверю, соответствует ли загруженная информация данной странице
                    if(extendedDistributionInfo.PageHref.equals(mSelectedDistribution.Href)){
                        Log.d("surprise", "TorrentDetailsFragment onChanged: have extended info of this element");
                        App.getInstance().ExtendedDistributionInfo.removeObserver(FullInfoObserver);
                        mSelectedDistribution.ExtendedInfo = extendedDistributionInfo;
                        // перерисую данные о торренте

                    }
                }
            }
        };

        extendedInfoContainer.observeForever(FullInfoObserver);
    }

    private void initializeBackground(Distribution data) {
        if(data.ExtendedInfo != null && data.ExtendedInfo.PostImageUrl != null){
            mDetailsBackground.enableParallax();
            /*Glide.with(getActivity())
                    .load(data.ExtendedInfo.PostImageUrl)
                    .into(new SimpleTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(Bitmap bitmap,
                                                    GlideAnimation<? super Bitmap> glideAnimation) {
                            mDetailsBackground.setCoverBitmap(bitmap);
                            mAdapter.notifyArrayItemRangeChanged(0, mAdapter.size());
                        }
                    });*/
        }
    }

   private void setupDetailsOverviewRow() {
       // настройка фрагмента с деталями
        final DetailsOverviewRow row = new DetailsOverviewRow(mSelectedDistribution);
        row.setImageDrawable(
                ContextCompat.getDrawable(getActivity(), R.drawable.default_background));
        int width = convertDpToPixel(getActivity().getApplicationContext(), DETAIL_THUMB_WIDTH);
        int height = convertDpToPixel(getActivity().getApplicationContext(), DETAIL_THUMB_HEIGHT);
        /*Glide.with(getActivity())
                .load(mSelectedDistribution.getCardImageUrl())
                .centerCrop()
                .error(R.drawable.default_background)
                .into(new SimpleTarget<GlideDrawable>(width, height) {
                    @Override
                    public void onResourceReady(GlideDrawable resource,
                                                GlideAnimation<? super GlideDrawable>
                                                        glideAnimation) {
                        row.setImageDrawable(resource);
                        mAdapter.notifyArrayItemRangeChanged(0, mAdapter.size());
                    }
                });*/

        ArrayObjectAdapter actionAdapter = new ArrayObjectAdapter();

        actionAdapter.add(
                new Action(
                        ACTION_DOWNLOAD_TORRENT,
                        getResources().getString(R.string.download_torrent)));
        actionAdapter.add(
                new Action(
                        ACTION_LOAD_MAGNET,
                        getResources().getString(R.string.rent_1),
                        getResources().getString(R.string.rent_2)));
        actionAdapter.add(
                new Action(
                        ACTION_SOME_ELSE,
                        getResources().getString(R.string.buy_1),
                        getResources().getString(R.string.buy_2)));
        row.setActionsAdapter(actionAdapter);
        mAdapter.add(row);
    }

    private void setupDetailsOverviewRowPresenter() {
        // Set detail background.
        FullWidthDetailsOverviewRowPresenter detailsPresenter =
                new FullWidthDetailsOverviewRowPresenter(new DetailsDescriptionPresenter());
        detailsPresenter.setBackgroundColor(
                ContextCompat.getColor(getActivity(), R.color.selected_background));

        // Hook up transition element.
        FullWidthDetailsOverviewSharedElementHelper sharedElementHelper =
                new FullWidthDetailsOverviewSharedElementHelper();
        sharedElementHelper.setSharedElementEnterTransition(
                getActivity(), DetailsActivity.SHARED_ELEMENT_NAME);
        detailsPresenter.setListener(sharedElementHelper);
        detailsPresenter.setParticipatingEntranceTransition(true);

        detailsPresenter.setOnActionClickedListener(new OnActionClickedListener() {
            @Override
            public void onActionClicked(Action action) {
                // тут будет какое-то действие
                Log.d("surprise", "TorrentDetailsFragment onActionClicked click");
                if(action.toString().equals(getString(R.string.download_torrent))){
                    Log.d("surprise", "TorrentDetailsFragment onActionClicked click on torrent");
                    // скачаю торрент
                    App.getInstance().downloadTorrent(mSelectedDistribution.TorrentHref);
                }
            }
        });
        mPresenterSelector.addClassPresenter(DetailsOverviewRow.class, detailsPresenter);
    }

    private void setupRelatedMovieListRow() {
        String[] subcategories = {getString(R.string.related_torrents)};
        List<Torrent> list = TorrentList.getList();

        Collections.shuffle(list);
        ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(new CardPresenter());
        for (int j = 0; j < NUM_COLS; j++) {
            listRowAdapter.add(list.get(j % 5));
        }

        HeaderItem header = new HeaderItem(0, subcategories[0]);
        mAdapter.add(new ListRow(header, listRowAdapter));
        mPresenterSelector.addClassPresenter(ListRow.class, new ListRowPresenter());
    }

    private int convertDpToPixel(Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(
                Presenter.ViewHolder itemViewHolder,
                Object item,
                RowPresenter.ViewHolder rowViewHolder,
                Row row) {

            if (item instanceof Distribution) {
                Intent intent = new Intent(getActivity(), DetailsActivity.class);
                intent.putExtra(getResources().getString(R.string.torrent), mSelectedDistribution);
                Bundle bundle =
                        ActivityOptionsCompat.makeSceneTransitionAnimation(
                                getActivity(),
                                ((ImageCardView) itemViewHolder.view).getMainImageView(),
                                DetailsActivity.SHARED_ELEMENT_NAME)
                                .toBundle();
                getActivity().startActivity(intent, bundle);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        App.getInstance().ExtendedDistributionInfo.removeObserver(FullInfoObserver);
    }
}
