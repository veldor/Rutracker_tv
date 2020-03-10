package net.veldor.rutrackertv.ui;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.Toast;

import androidx.core.app.ActivityOptionsCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.leanback.app.DetailsSupportFragment;
import androidx.leanback.widget.Action;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.BaseOnItemViewClickedListener;
import androidx.leanback.widget.ClassPresenterSelector;
import androidx.leanback.widget.DetailsOverviewRow;
import androidx.leanback.widget.FullWidthDetailsOverviewRowPresenter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.OnActionClickedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.RowPresenter;
import androidx.leanback.widget.SparseArrayObjectAdapter;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import net.veldor.rutrackertv.App;
import net.veldor.rutrackertv.CardPresenter;
import net.veldor.rutrackertv.DetailsDescriptionPresenter;
import net.veldor.rutrackertv.R;
import net.veldor.rutrackertv.TorrentList;
import net.veldor.rutrackertv.selections.Distribution;
import net.veldor.rutrackertv.selections.ExtendedDistributionInfo;

import java.util.ArrayList;

@SuppressWarnings("unused")
public class NewTorrentDetailsFragment extends DetailsSupportFragment {
    private static final int ACTION_DOWNLOAD_TORRENT = 1;
    private static final int ACTION_SEARCH_IN_CATEGORY =2;
    private Distribution mSelectedDistribution;
    private ArrayObjectAdapter mRowsAdapter;
    private DetailsOverviewRow mDetailsOverview;


    private static final int DETAIL_THUMB_WIDTH = 500;
    private static final int DETAIL_THUMB_HEIGHT = 500;
    private FragmentActivity mActivity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildDetails();
        handleExtendedInfoLoad();
    }

    private void buildDetails() {
        mActivity = getActivity();
        if (mActivity != null) {
            mSelectedDistribution = (Distribution) mActivity.getIntent().getSerializableExtra(DetailsActivity.DISTRIBUTION);
            ClassPresenterSelector selector = new ClassPresenterSelector();

            FullWidthDetailsOverviewRowPresenter rowPresenter =
                    new FullWidthDetailsOverviewRowPresenter(
                            new DetailsDescriptionPresenter());
            selector.addClassPresenter(DetailsOverviewRow.class, rowPresenter);
            selector.addClassPresenter(ListRow.class,
                    new ListRowPresenter());
            mRowsAdapter = new ArrayObjectAdapter(selector);
            Resources res = getActivity().getResources();
            mDetailsOverview = new DetailsOverviewRow(mSelectedDistribution);

            // Add images and action buttons to the details view
            mDetailsOverview.setImageDrawable(res.getDrawable(R.drawable.torrent));
            SparseArrayObjectAdapter adapter = new SparseArrayObjectAdapter();
            adapter.set(ACTION_DOWNLOAD_TORRENT, new Action(ACTION_DOWNLOAD_TORRENT, getResources().getString(
                    R.string.download_torrent)));
            adapter.set(ACTION_SEARCH_IN_CATEGORY, new Action(ACTION_SEARCH_IN_CATEGORY, getResources().getString(
                    R.string.search_in_this_category_message)));
            mDetailsOverview.setActionsAdapter(adapter);

            rowPresenter.setOnActionClickedListener(new OnActionClickedListener() {
                @Override
                public void onActionClicked(Action action) {
                    if (action.toString().equals(getString(R.string.download_torrent))) {
                        action.setLabel1("Torrent loaded");
                        Toast.makeText(App.getInstance(), R.string.begin_torrent_download_message, Toast.LENGTH_SHORT).show();
                        // скачаю торрент
                        App.getInstance().downloadTorrent(mSelectedDistribution.TorrentHref);
                    }
                    else if (action.toString().equals(getString(R.string.search_in_this_category_message))) {
                        action.setLabel1("Torrent loaded");
                        Toast.makeText(App.getInstance(), R.string.start_search, Toast.LENGTH_SHORT).show();
                        App.getInstance().mSelectedCategory = mSelectedDistribution.Category;
                        App.getInstance().search();
                        mActivity.finish();
                    }
                }
            });

            mRowsAdapter.add(mDetailsOverview);
            // ========================================================================
            // Прикреплю результаты поиска
            ArrayList<Distribution> distributions = App.getInstance().SearchedDistributions.getValue();
            if(distributions != null && distributions.size() > 0){
                ArrayObjectAdapter rowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
                CardPresenter cardPresenter = new CardPresenter();

                ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(cardPresenter);
                    for (Distribution distribution : distributions) {
                        listRowAdapter.add(distribution);
                    }
                HeaderItem header = new HeaderItem(0, TorrentList.TORRENT_CATEGORY[0]);
                mRowsAdapter.add(new ListRow(header, listRowAdapter));
            }
            // ========================================================================
            setAdapter(mRowsAdapter);

            setOnItemViewClickedListener(new ItemViewClickedListener());
        }
    }


    private void handleExtendedInfoLoad() {
        LiveData<ExtendedDistributionInfo> extendedInfoContainer = App.getInstance().ExtendedDistributionInfo;

        // проверю, соответствует ли загруженная информация данной странице
        // перерисую данные о торренте
        Observer<ExtendedDistributionInfo> fullInfoObserver = new Observer<ExtendedDistributionInfo>() {
            @Override
            public void onChanged(ExtendedDistributionInfo extendedDistributionInfo) {
                if (extendedDistributionInfo != null) {
                    // проверю, соответствует ли загруженная информация данной странице
                    if (extendedDistributionInfo.PageHref.equals(mSelectedDistribution.Href)) {
                        mSelectedDistribution.ExtendedInfo = extendedDistributionInfo;
                        mRowsAdapter.notifyArrayItemRangeChanged(0, mRowsAdapter.size());
                        // перерисую данные о торренте
                        changePreviewImage(mSelectedDistribution);
                    }
                }
            }
        };

        extendedInfoContainer.observe(this, fullInfoObserver);
    }

    private void changePreviewImage(Distribution data) {
        if (data.ExtendedInfo != null && data.ExtendedInfo.PostImageUrl != null) {
            int width = convertDpToPixel(App.getInstance(), DETAIL_THUMB_WIDTH);
            int height = convertDpToPixel(App.getInstance(), DETAIL_THUMB_HEIGHT);
        }

    }

    private int convertDpToPixel(Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

    private class ItemViewClickedListener implements BaseOnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item, RowPresenter.ViewHolder rowViewHolder, Object row) {
            if(item instanceof Distribution){
                Intent intent = new Intent(getActivity(), DetailsActivity.class);
                intent.putExtra(DetailsActivity.DISTRIBUTION, ((Distribution) item));
                Bundle bundle =
                        ActivityOptionsCompat.makeSceneTransitionAnimation(
                                mActivity,
                                ((ImageCardView) itemViewHolder.view).getMainImageView(),
                                DetailsActivity.SHARED_ELEMENT_NAME)
                                .toBundle();
                mActivity.startActivity(intent, bundle);
                mActivity.finish();
            }
        }
    }
}
