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

package net.veldor.rutrackertv;

import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.Presenter;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import net.veldor.rutrackertv.selections.ContentTypes;
import net.veldor.rutrackertv.selections.Distribution;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;
import static androidx.leanback.widget.BaseCardView.CARD_TYPE_INFO_UNDER_WITH_EXTRA;

/*
 * A CardPresenter is used to generate Views and bind Objects to them on demand.
 * It contains an Image CardView
 */
public class CardPresenter extends Presenter {

    private static final int CARD_WIDTH = 500;
    private static final int CARD_HEIGHT = 500;
    private static int sSelectedBackgroundColor;
    private static int sDefaultBackgroundColor;

    private static void updateCardBackgroundColor(ImageCardView view, boolean selected) {
        int color = selected ? sSelectedBackgroundColor : sDefaultBackgroundColor;
        // Both background colors should be set because the view's background is temporarily visible
        // during animations.
        view.setBackgroundColor(color);
        view.findViewById(R.id.info_field).setBackgroundColor(color);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {

        sDefaultBackgroundColor =
                ContextCompat.getColor(parent.getContext(), R.color.default_background);
        sSelectedBackgroundColor =
                ContextCompat.getColor(parent.getContext(), R.color.selected_background);

        ImageCardView cardView =
                new ImageCardView(parent.getContext()) {
                    @Override
                    public void setSelected(boolean selected) {
                        updateCardBackgroundColor(this, selected);
                        super.setSelected(selected);
                    }
                };

        cardView.setFocusable(true);
        cardView.setFocusableInTouchMode(true);
        LayoutInflater inflater = (LayoutInflater) App.getInstance().getSystemService(LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.card_additional_block, null);
        cardView.addView(view);
        updateCardBackgroundColor(cardView, false);
        return new ViewHolder(cardView);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        final Distribution distribution = (Distribution) item;

        if (distribution.ExtendedInfo != null) {
            // проверю разрешение файла
            if (distribution.ExtendedInfo.FileName != null) {
                distribution.ContentType = ContentTypes.getMovieFormat(distribution.ExtendedInfo.FileName);
                distribution.IsMovie = ContentTypes.isMovie(distribution.ContentType);
            }
        }

        ImageCardView cardView = (ImageCardView) viewHolder.view;
        cardView.setCardType(CARD_TYPE_INFO_UNDER_WITH_EXTRA);

        TextView sizeView = cardView.findViewById(R.id.size);
        sizeView.setText(distribution.Size);
        TextView seedsView = cardView.findViewById(R.id.seeds);
        seedsView.setText(distribution.Seeds);
        TextView isMovieView = cardView.findViewById(R.id.isMovie);
        if (distribution.IsMovie) {
            isMovieView.setText(distribution.ContentType);
        } else {
            isMovieView.setText("--");
        }
        cardView.setTitleText(distribution.Category);
        cardView.setContentText(distribution.Name);
        cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT);
        if (distribution.ExtendedInfo != null && distribution.ExtendedInfo.PostImageUrl != null) {
            if (distribution.IsMovie) {
                cardView.setMainImage(App.getInstance().getDrawable(R.drawable.movie));
                // Загружаю картинку
                Glide.with(viewHolder.view.getContext())
                        .load(distribution.ExtendedInfo.PostImageUrl)
                        .apply(new RequestOptions()
                                .placeholder(R.drawable.clapper)
                                .fitCenter()
                        )
                        .into(cardView.getMainImageView());
            }
        } else {
            if (distribution.IsMovie) {
                cardView.setMainImage(App.getInstance().getDrawable(R.drawable.movie));
            } else {
                cardView.setMainImage(App.getInstance().getDrawable(R.drawable.torrent));
            }
        }

        /*Data inputData = new Data.Builder()
                .putString(PageDetailsWorker.PAGE_HREF, distribution.Href)
                .build();
        // начну загружать информацию о странице
        OneTimeWorkRequest pageDetalisWork = new OneTimeWorkRequest.Builder(PageDetailsWorker.class).setInputData(inputData).build();
        WorkManager.getInstance(App.getInstance()).enqueue(pageDetalisWork);*/


        cardView.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
                MenuItem myActionItem = contextMenu.add(R.string.search_in_this_category_message);
                myActionItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        // вызову поиск по данной категории
                        App.getInstance().mSelectedCategory = distribution.Category;
                        App.getInstance().search();
                        return false;
                    }
                });
            }
        });
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {
        ImageCardView cardView = (ImageCardView) viewHolder.view;
        // Remove references to images so that the garbage collector can free up memory
        cardView.setBadgeImage(null);
        cardView.setMainImage(null);
    }
}
