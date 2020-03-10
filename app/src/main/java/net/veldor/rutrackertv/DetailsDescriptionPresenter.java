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

import android.text.Html;
import android.text.Spanned;

import net.veldor.rutrackertv.selections.Distribution;
import net.veldor.rutrackertv.utils.MyAbstractDetailsDescriptionPresenter;

public class DetailsDescriptionPresenter extends MyAbstractDetailsDescriptionPresenter {

    @Override
    protected void onBindDescription(ViewHolder viewHolder, Object item) {
        Distribution distribution = (Distribution) item;
        if (distribution != null) {
            if (distribution.ExtendedInfo != null && distribution.ExtendedInfo.FileName != null) {
                viewHolder.getTitle().setText(distribution.ExtendedInfo.FileName);
            } else {
                viewHolder.getTitle().setText(distribution.Category);
            }
            viewHolder.getSubtitle().setText(distribution.Name);
            if (distribution.ExtendedInfo != null && distribution.ExtendedInfo.PostBody != null) {
                Spanned body = Html.fromHtml(distribution.ExtendedInfo.PostBody);
                viewHolder.getBody().setText(body);
            } else {
                viewHolder.getBody().setText("Тут появится описание торрента");
            }
        }
    }
}
