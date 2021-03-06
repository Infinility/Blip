/*
 * Copyright 2015, Tanmay Parikh
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tanmay.blip.fragments;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.picasso.OkHttpDownloader;
import com.squareup.picasso.Picasso;
import com.tanmay.blip.BlipApplication;
import com.tanmay.blip.R;
import com.tanmay.blip.activities.AboutActivity;
import com.tanmay.blip.activities.DonateActivity;
import com.tanmay.blip.activities.ImageActivity;
import com.tanmay.blip.activities.SearchActivity;
import com.tanmay.blip.activities.SettingsActivity;
import com.tanmay.blip.database.DatabaseManager;
import com.tanmay.blip.database.SharedPrefs;
import com.tanmay.blip.models.Comic;
import com.tanmay.blip.utils.BlipUtils;
import com.tanmay.blip.utils.SpeechSynthesizer;
import com.tanmay.blip.views.EndlessRecyclerOnScrollListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class FeedFragment extends Fragment {

    private RecyclerView recyclerView;
    private FeedListAdapter adapter;
    private DatabaseManager databaseManager;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_feed, container, false);

        if (SharedPrefs.getInstance().isNightModeEnabled()) {
            rootView.setBackgroundColor(getActivity().getResources().getColor(R.color.primary_light_night));
        }

        setHasOptionsMenu(true);

        recyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerview);
        StaggeredGridLayoutManager layoutManager;
        if (getResources().getBoolean(R.bool.landscape)) {
            layoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        } else {
            layoutManager = new StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL);
        }
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addOnScrollListener(new EndlessRecyclerOnScrollListener(layoutManager) {
            @Override
            public void onLoadMore(int current_page) {
                if (adapter != null && databaseManager != null) {
                    adapter.addComics(databaseManager.getFeed(adapter.getLastItemNum()));
                }
            }
        });

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (databaseManager == null) {
            databaseManager = new DatabaseManager(getActivity());
        }

        if (recyclerView.getAdapter() == null) {
            adapter = new FeedListAdapter();
            adapter.addComics(databaseManager.getFeed());
            recyclerView.setAdapter(adapter);
        } else {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_search, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.search:
                startActivity(new Intent(getActivity(), SearchActivity.class));
                return true;
            case R.id.about:
                startActivity(new Intent(getActivity(), AboutActivity.class));
                return true;
            case R.id.settings:
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            case R.id.donate:
                startActivity(new Intent(getActivity(), DonateActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public class FeedListAdapter extends RecyclerView.Adapter<FeedListAdapter.ViewHolder> {

        private List<Comic> comics;
        private SimpleDateFormat simpleDateFormat;

        public FeedListAdapter() {
            simpleDateFormat = new SimpleDateFormat("MMMM dd, yyyy (EEEE)", Locale.getDefault());
            OkHttpClient picassoClient = BlipApplication.getInstance().client.clone();
            picassoClient.interceptors().add(BlipUtils.REWRITE_CACHE_CONTROL_INTERCEPTOR);
            new Picasso.Builder(getActivity()).downloader(new OkHttpDownloader(picassoClient)).build();
        }

        public void addComics(List<Comic> comics) {
            if (this.comics == null) {
                this.comics = comics;
            } else {
                this.comics.addAll(comics);
            }
        }

        public int getLastItemNum() {
            return comics.get(comics.size() - 1).getNum();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getActivity().getLayoutInflater().inflate(R.layout.item_comic, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Comic comic = comics.get(position);

            if (SharedPrefs.getInstance().isNightModeEnabled()) {
                holder.backgroundCard.setCardBackgroundColor(getActivity().getResources().getColor(R.color.primary_night));
                holder.title.setTextColor(getActivity().getResources().getColor(android.R.color.white));
                holder.date.setTextColor(getActivity().getResources().getColor(android.R.color.white));
                holder.alt.setTextColor(getActivity().getResources().getColor(android.R.color.white));
                holder.transcript.setColorFilter(getActivity().getResources().getColor(android.R.color.white));
                holder.share.setColorFilter(getActivity().getResources().getColor(android.R.color.white));
                holder.explain.setColorFilter(getActivity().getResources().getColor(android.R.color.white));
                holder.browser.setColorFilter(getActivity().getResources().getColor(android.R.color.white));
            }

            String title;
            if (SharedPrefs.getInstance().isTitleHidden()) {
                title = String.valueOf(comic.getNum());
            } else {
                title = comic.getNum() + ". " + comic.getTitle();
            }
            holder.title.setText(title);

            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.YEAR, Integer.parseInt(comic.getYear()));
            calendar.set(Calendar.MONTH, Integer.parseInt(comic.getMonth()) - 1);
            calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(comic.getDay()));
            holder.date.setText(simpleDateFormat.format(calendar.getTime()));

            if (SharedPrefs.getInstance().isAltSpoilerized()) {
                String altText = getResources().getString(R.string.title_pager_alt_spoiler);
                holder.alt.setClickable(true);
                holder.alt.setText(altText);
            } else {
                holder.alt.setClickable(false);
                holder.alt.setText(comic.getAlt());
            }

            Picasso.with(holder.img.getContext())
                    .load(comic.getImg())
                    .error(R.drawable.error_network)
                    .into(holder.img);

            if (comic.isFavourite()) {
                holder.favourite.setColorFilter(getResources().getColor(R.color.accent));
            } else {
                if (SharedPrefs.getInstance().isNightModeEnabled()) {
                    holder.favourite.setColorFilter(getResources().getColor(android.R.color.white));
                } else {
                    holder.favourite.setColorFilter(getResources().getColor(R.color.icons_dark));
                }
            }
        }

        @Override
        public int getItemCount() {
            return comics.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

            TextView title, date, alt;
            ImageView img, favourite, transcript, share, explain, browser;
            View imgContainer;
            CardView backgroundCard;

            public ViewHolder(View itemView) {
                super(itemView);
                title = (TextView) itemView.findViewById(R.id.title);
                date = (TextView) itemView.findViewById(R.id.date);
                alt = (TextView) itemView.findViewById(R.id.alt);
                img = (ImageView) itemView.findViewById(R.id.img);
                favourite = (ImageView) itemView.findViewById(R.id.favourite);
                backgroundCard = (CardView) itemView;
                browser = (ImageView) itemView.findViewById(R.id.open_in_browser);
                transcript = (ImageView) itemView.findViewById(R.id.transcript);
                imgContainer = itemView.findViewById(R.id.img_container);
                share = (ImageView) itemView.findViewById(R.id.share);
                explain = (ImageView) itemView.findViewById(R.id.help);

                browser.setOnClickListener(this);
                transcript.setOnClickListener(this);
                imgContainer.setOnClickListener(this);
                favourite.setOnClickListener(this);
                share.setOnClickListener(this);
                explain.setOnClickListener(this);
                alt.setOnClickListener(this);
            }

            @Override
            public void onClick(View v) {
                final int position = getAdapterPosition();
                switch (v.getId()) {
                    case R.id.open_in_browser:
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://xkcd.com/" + comics.get(position).getNum()));
                        startActivity(intent);
                        break;
                    case R.id.transcript:
                        String content = comics.get(position).getTranscript();
                        if (content.equals("")) {
                            content = getResources().getString(R.string.message_no_transcript);
                        }
                        final String speakingContent = content;
                        new MaterialDialog.Builder(getActivity())
                                .title(R.string.title_dialog_transcript)
                                .content(content)
                                .negativeText(R.string.negative_text_dialog)
                                .neutralText(R.string.neutral_text_dialog_speak)
                                .autoDismiss(false)
                                .callback(new MaterialDialog.ButtonCallback() {
                                    @Override
                                    public void onNegative(MaterialDialog dialog) {
                                        super.onNegative(dialog);
                                        dialog.dismiss();
                                    }

                                    @Override
                                    public void onNeutral(MaterialDialog dialog) {
                                        super.onNeutral(dialog);
                                        SpeechSynthesizer.getInstance().convertToSpeechFlush(speakingContent);
                                    }
                                })
                                .dismissListener(new DialogInterface.OnDismissListener() {
                                    @Override
                                    public void onDismiss(DialogInterface dialog) {
                                        SpeechSynthesizer.getInstance().stopSpeaking();
                                    }
                                })
                                .show();
                        break;
                    case R.id.img_container:
                        ImageActivity.launch((AppCompatActivity) getActivity(), img, comics.get(position).getNum());
                        break;
                    case R.id.favourite:
                        boolean fav = comics.get(position).isFavourite();
                        comics.get(position).setFavourite(!fav);
                        databaseManager.setFavourite(comics.get(position).getNum(), !fav);
                        if (fav) {
                            if (SharedPrefs.getInstance().isNightModeEnabled()) {
                                favourite.setColorFilter(getResources().getColor(android.R.color.white));
                            } else {
                                favourite.setColorFilter(getResources().getColor(R.color.icons_dark));
                            }
                        } else {
                            //make fav
                            favourite.setColorFilter(getResources().getColor(R.color.accent));
                        }
                        break;
                    case R.id.help:
                        Intent explainIntent = new Intent(Intent.ACTION_VIEW,
                                Uri.parse("http://www.explainxkcd.com/wiki/index.php/" + comics.get(position).getNum()));
                        startActivity(explainIntent);
                        break;
                    case R.id.share:
                        Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
                        shareIntent.setType("text/plain");
                        if (BlipUtils.isLollopopUp()) {
                            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
                        } else {
                            shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                        }
                        shareIntent.putExtra(Intent.EXTRA_SUBJECT, comics.get(position).getTitle());
                        shareIntent.putExtra(Intent.EXTRA_TEXT, comics.get(position).getImg());
                        startActivity(Intent.createChooser(shareIntent, getActivity().getResources().getString(R.string.tip_share_image_url)));
                        break;
                    case R.id.alt:
                        alt.setText(comics.get(position).getAlt());
                        alt.setClickable(false);
                        break;
                }
            }
        }

    }
}
