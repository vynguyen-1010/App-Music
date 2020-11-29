package com.fhm.musicr.ui.page.librarypage.playlist;

import android.graphics.Bitmap;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.fhm.musicr.R;
import com.fhm.musicr.ui.BaseActivity;
import com.fhm.musicr.ui.page.BaseMusicServiceFragment;
import com.fhm.musicr.ui.page.librarypage.song.SongSearchEvent;
import com.fhm.musicr.ui.page.subpages.PlaylistPagerFragment;
import com.fhm.musicr.loader.medialoader.PlaylistLoader;
import com.fhm.musicr.model.Playlist;
import com.fhm.musicr.ui.page.featurepage.FeaturePlaylistAdapter;
import com.fhm.musicr.ui.widget.fragmentnavigationcontroller.SupportFragment;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.BindView;
import butterknife.ButterKnife;

public class PlaylistChildTab extends BaseMusicServiceFragment implements FeaturePlaylistAdapter.PlaylistClickListener {
    public static final String TAG ="PlaylistChildTab";

    @BindView(R.id.recycler_view)
    RecyclerView mRecyclerView;
    PlaylistChildAdapter mAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.playlist_child_tab,container,false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this,view);
        mAdapter = new PlaylistChildAdapter(getActivity(),true);
        mAdapter.setOnItemClickListener(this);
        mRecyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        mRecyclerView.setAdapter(mAdapter);

        if(getActivity() instanceof BaseActivity) {
            ((BaseActivity)getActivity()).addMusicServiceEventListener(this);
        }
        refreshData();


;    }



    private void refreshData() {
        if(getActivity() !=null)
            mAdapter.setData(PlaylistLoader.getAllPlaylistsWithAuto(getActivity()));
    }



    @Override
    public void onClickPlaylist(Playlist playlist, @org.jetbrains.annotations.Nullable Bitmap bitmap) {
        SupportFragment sf = PlaylistPagerFragment.newInstance(getContext(),playlist,bitmap);
        Fragment parentFragment = getParentFragment();
        if(parentFragment instanceof SupportFragment)
            ((SupportFragment)parentFragment).getNavigationController().presentFragment(sf);
    }

    @Override
    public void onMediaStoreChanged() {
        refreshData();
    }


}