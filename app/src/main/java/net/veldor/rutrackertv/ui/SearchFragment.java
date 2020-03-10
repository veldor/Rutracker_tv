/*
 * Copyright (c) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.veldor.rutrackertv.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.leanback.app.SearchSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.ObjectAdapter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.leanback.widget.SearchEditText;
import androidx.lifecycle.ViewModelProvider;

import net.veldor.rutrackertv.App;
import net.veldor.rutrackertv.R;
import net.veldor.rutrackertv.utils.FileHandler;
import net.veldor.rutrackertv.viewmodel.MyViewModel;

import java.util.ArrayList;

/*
 * This class demonstrates how to do in-app search
 */
public class SearchFragment extends SearchSupportFragment
        implements SearchSupportFragment.SearchResultProvider{

    private static final boolean FINISH_ON_RECOGNIZER_CANCELED = true;
    private static final int REQUEST_SPEECH = 0x00000010;
    public static final int SEARCH_WAIT = 2;

    private static final int GRID_ITEM_WIDTH = 500;
    private static final int GRID_ITEM_HEIGHT = 200;

    private MyViewModel ViewModel;
    private ArrayObjectAdapter mRowsAdapter;
    private SearchEditText mSearchEdivView;
    private View mSearchView;
    private ListRow mSearchRow;
    private ArrayObjectAdapter mGridRowAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModel = new ViewModelProvider(this).get(MyViewModel.class);
        setSearchResultProvider(this);
        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        showLastSearchResults();

        setOnItemViewClickedListener(new ItemViewClickedListener());
    }

    private void setupUI() {
        View view = getView();
        if(view != null){
            mSearchView = view.findViewById(R.id.lb_search_bar);
            mSearchEdivView = view.findViewById(R.id.lb_search_text_editor);
        }
    }

    @Override
    public void onAttachFragment(@NonNull Fragment childFragment) {
        super.onAttachFragment(childFragment);
        setupUI();
    }

    private void showLastSearchResults() {
        HeaderItem gridHeader = new HeaderItem(0, getString(R.string.last_results_message));
        GridItemPresenter mGridPresenter = new GridItemPresenter();
        mGridRowAdapter = new ArrayObjectAdapter(mGridPresenter);
        // получу сохранённые результаты
        ArrayList<String> mSavedResults = FileHandler.getSearchAutocomplete();
        if(mSavedResults != null && mSavedResults.size() > 0){
            for(String s : mSavedResults){
                mGridRowAdapter.add(s);
            }
        }
        mSearchRow = new ListRow(gridHeader, mGridRowAdapter);
        mRowsAdapter.add(mSearchRow);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SPEECH) {
            if (resultCode == Activity.RESULT_OK) {
                setSearchQuery(data, true);
            } else {// If recognizer is canceled or failed, keep focus on the search orb
                if (FINISH_ON_RECOGNIZER_CANCELED) {
                    View view = getView();
                        if(view != null){
                            View searchBar = view.findViewById(R.id.lb_search_bar_speech_orb);
                            searchBar.requestFocus();
                        }
                }
            }
        }
        else if(requestCode == SEARCH_WAIT){
            if (resultCode == Activity.RESULT_OK) {
                // результат получен, закрываю активити
                FragmentActivity activity = getActivity();
                if(activity != null && !activity.isFinishing()){
                    activity.finish();
                }
            }
            else{
                Log.d("surprise", "SearchFragment onActivityResult ");
            }
        }
    }


    @Override
    public ObjectAdapter getResultsAdapter() {
        return mRowsAdapter;
    }

    // запрос изменён
    @Override
    public boolean onQueryTextChange(String newQuery) {
        return true;
    }

    // запрос подтверждён
    @Override
    public boolean onQueryTextSubmit(String query) {
        if (!TextUtils.isEmpty(query) && !query.equals("nil")) {
            // сохнаню запрос
            FileHandler.putSearchValue(query);
            // выполню запрос
            App.getInstance().SearchWorkStatus = ViewModel.search(query);
            startActivityForResult(new Intent(getActivity(), SearchWaiterActivity.class), SEARCH_WAIT);
        }
        else{
            Toast.makeText(getContext(), "Пустой поисковый запрос. Напишите что-нибудь", Toast.LENGTH_SHORT).show();
        }
        return true;
    }


    // даю фокус окну ввода поиска
    void focusOnSearch() {
        mSearchView.requestFocus();
    }

    private class GridItemPresenter extends Presenter {
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
            viewHolder.view.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
                @Override
                public void onCreateContextMenu(ContextMenu contextMenu, final View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
                    MenuItem removeResultItem = contextMenu.add(R.string.remove_item_message);
                    removeResultItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem menuItem) {
                            // удалю элемент
                            FileHandler.removeFromHistory(((TextView)view).getText());
                            mGridRowAdapter.remove(((TextView)view).getText());
                            mRowsAdapter.notifyArrayItemRangeChanged(0, mRowsAdapter.size());
                            return false;
                        }
                    });
                    MenuItem clearHistoryItem = contextMenu.add(R.string.clear_history_message);
                    clearHistoryItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem menuItem) {
                            // удалю историю
                            FileHandler.clearSearchHistory();
                            Toast.makeText(App.getInstance(), R.string.search_history_clear_message, Toast.LENGTH_LONG).show();
                            mRowsAdapter.remove(mSearchRow);
                            mRowsAdapter.notifyArrayItemRangeChanged(0, mRowsAdapter.size());
                            return false;
                        }
                    });
                }
            });
        }

        @Override
        public void onUnbindViewHolder(ViewHolder viewHolder) {
        }
    }

    private class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item, RowPresenter.ViewHolder rowViewHolder, Row row) {
            // добавлю данный текст в поле ввода
            if(mSearchEdivView != null){
                mSearchEdivView.setText(item.toString());
                mSearchEdivView.requestFocus();
            }
        }
    }
}
