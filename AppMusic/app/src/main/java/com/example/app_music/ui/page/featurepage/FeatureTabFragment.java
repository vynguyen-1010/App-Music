package com.fhm.musicr.ui.page.featurepage;

import android.graphics.Bitmap;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.fhm.musicr.R;
import com.fhm.musicr.service.MusicServiceEventListener;
import com.fhm.musicr.ui.page.BaseMusicServiceSupportFragment;
import com.fhm.musicr.ui.page.subpages.PlaylistPagerFragment;
import com.fhm.musicr.loader.medialoader.PlaylistLoader;
import com.fhm.musicr.loader.medialoader.SongLoader;
import com.fhm.musicr.model.Playlist;
import com.fhm.musicr.ui.widget.fragmentnavigationcontroller.SupportFragment;

import butterknife.BindView;
import butterknife.ButterKnife;

public class FeatureTabFragment extends BaseMusicServiceSupportFragment implements FeaturePlaylistAdapter.PlaylistClickListener, MusicServiceEventListener {
    private static final String TAG ="FeatureTabFragment";

    @BindView(R.id.status_bar)
    View mStatusView;

    @BindView(R.id.swipe_refresh)
    SwipeRefreshLayout mSwipeRefreshLayout;

    @BindView(R.id.scroll_view)
    NestedScrollView mNestedScrollView;

    FeatureLinearHolder mFeatureLinearHolder;

    @Nullable
    @Override
    protected View onCreateView(LayoutInflater inflater, ViewGroup container) {
        return inflater.inflate(R.layout.feature_tab_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this,view);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.FlatOrange);
        mSwipeRefreshLayout.setOnRefreshListener(this::refreshData);

        mFeatureLinearHolder = new FeatureLinearHolder(getActivity(),mNestedScrollView);
        mFeatureLinearHolder.setPlaylistItemClick(this);

        refreshData();
    }

    private void refreshData() {

        if(getActivity()!=null) {
            mFeatureLinearHolder.setSuggestedPlaylists(PlaylistLoader.getAllPlaylistsWithAuto(getActivity()));
            mFeatureLinearHolder.setSuggestedSongs(SongLoader.getAllSongs(getActivity()));
        }

        mSwipeRefreshLayout.setRefreshing(false);

    }

    @Override
    public void onSetStatusBarMargin(int value) {
        if(mStatusView!=null) {
            mStatusView.getLayoutParams().height = value;
            mStatusView.requestLayout();
        }
    }

    @Override
    public void onClickPlaylist(Playlist playlist, @org.jetbrains.annotations.Nullable Bitmap bitmap) {
        SupportFragment sf = PlaylistPagerFragment.newInstance(getContext(),playlist,bitmap);
        getNavigationController().presentFragment(sf);
    }

    @Override
    public void onServiceConnected() {

    }

    @Override
    public void onServiceDisconnected() {

    }

    @Override
    public void onQueueChanged() {

    }

    @Override
    public void onPlayingMetaChanged() {

    }

    @Override
    public void onPlayStateChanged() {

    }

    @Override
    public void onRepeatModeChanged() {

    }

    @Override
    public void onShuffleModeChanged() {

    }

    @Override
    public void onMediaStoreChanged() {
        refreshData();
    }

}
