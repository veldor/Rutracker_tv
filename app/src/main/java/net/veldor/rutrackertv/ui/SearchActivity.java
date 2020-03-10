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

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import net.veldor.rutrackertv.LeanbackActivity;
import net.veldor.rutrackertv.R;

/*
 * SearchActivity for SearchFragment
 */
public class SearchActivity extends LeanbackActivity {
    private SearchFragment mFragment;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search);
        mFragment = (SearchFragment) getSupportFragmentManager()
                .findFragmentById(R.id.search_fragment);
    }

    @Override
    public boolean onSearchRequested() {
        Log.d("surprise", "SearchActivity onSearchRequested have result");
            startActivity(new Intent(this, SearchActivity.class));
        return true;
    }
/*    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // If there are no results found, press the left key to reselect the microphone
        // берём в фокус поле поиска
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            mFragment.focusOnSearch();
        }
        return super.onKeyDown(keyCode, event);
    }*/
}
