package com.fhm.musicr.ui.page.librarypage.song;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.Group;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.fhm.musicr.App;
import com.fhm.musicr.R;
import com.fhm.musicr.contract.AbsMediaAdapter;
import com.fhm.musicr.loader.medialoader.SongLoader;
import com.fhm.musicr.model.Song;
import com.fhm.musicr.ui.page.BaseMusicServiceFragment;
import com.fhm.musicr.ui.bottomsheet.SortOrderBottomSheet;
import com.fhm.musicr.util.Tool;
import com.fhm.musicr.util.Util;
import com.fhm.musicr.util.Animation;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SongChildTab extends BaseMusicServiceFragment implements SortOrderBottomSheet.SortOrderChangedListener, PreviewRandomPlayAdapter.FirstItemCallBack{
    public static final String TAG ="SongChildTab";

//    public static SongChildTab songChildTab = null;
    SongChildAdapter mAdapter;

    @BindView(R.id.recycler_view)
    RecyclerView mRecyclerView;

//    @BindView(R.id.preview_shuffle_list)
//    RecyclerView mPreviewRecyclerView;

    @BindView(R.id.refresh)
    ImageView mRefresh;

    @BindView(R.id.image)
    ImageView mImage;

    @BindView(R.id.title)
    TextView mTitle;

    @BindView(R.id.description)
    TextView mArtist;

    @BindView(R.id.random_group)
    Group mRandomGroup;

    private int mCurrentSortOrder = 0;
    private void initSortOrder() {
         mCurrentSortOrder = App.getInstance().getPreferencesUtility().getSongChildSortOrder();
    }


    // ---- Khai bao dang ky nhan su kien dung EventBus --- //
    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    // ---- Khai bao huy dang ky nhan su kien dung EventBus --- //
    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    // Ham nhan su kien EventBus
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(SongSearchEvent event) {
        // Du lieu nhan duoc tu LibraryTabFragment
        // Goi ham searchMySong de tim kiem bai hat va load lai giao dien
        searchMySong(event.getMessage());
    };

    // Tim kiem bai hat va load lai giao dien
    public void searchMySong( String songName){
        ArrayList<Song> songs = SongLoader.getSongs(getActivity(), songName);
        mAdapter.setData(songs);
        showOrHidePreview(!songs.isEmpty());
    }

    @OnClick({R.id.preview_random_panel})
     void shuffle() {
        mAdapter.shuffle();
    }


//    PreviewRandomPlayAdapter mPreviewAdapter;

    @OnClick(R.id.refresh)
    void refresh() {
        mRefresh.animate().rotationBy(360).setInterpolator(Animation.getInterpolator(6)).setDuration(650);
        mRefresh.postDelayed(mAdapter::randomize,300);
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.song_child_tab,container,false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this,view);

        initSortOrder();

        mAdapter = new SongChildAdapter(getActivity());
        mAdapter.setName(TAG);
        mAdapter.setCallBack(this);
        mAdapter.setSortOrderChangedListener(this);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(),LinearLayoutManager.VERTICAL,false));
        mRecyclerView.setAdapter(mAdapter);

        refreshData();


    }


    @Override
    public void onDestroyView() {
        mAdapter.destroy();
        super.onDestroyView();
    }

    private void refreshData() {
    /*    if(getContext() != null)
        SongLoader.doSomething(getContext());
*/
        ArrayList<Song> songs = SongLoader.getAllSongs(getActivity(),SortOrderBottomSheet.mSortOrderCodes[mCurrentSortOrder]);

        mAdapter.setData(songs);
        showOrHidePreview(!songs.isEmpty());

    }
    private void showOrHidePreview(boolean show) {
        int v = show ? View.VISIBLE : View.GONE;

        mRandomGroup.setVisibility(v);
    }

    @Override
    public void onFirstItemCreated(Song song) {
        mTitle.setText(song.title);
        mArtist.setText(song.artistName);

        Glide.with(this)
                .load(Util.getAlbumArtUri(song.albumId))
                .placeholder(R.drawable.music_style)
                .error(R.drawable.music_empty)
                .into(mImage);

    }

    @Override
    public void onPlayingMetaChanged() {
        if(mAdapter!=null)mAdapter.notifyOnMediaStateChanged(AbsMediaAdapter.PLAY_STATE_CHANGED);
    }

    @Override
    public void onPaletteChanged() {
        if(mRecyclerView instanceof FastScrollRecyclerView) {
            FastScrollRecyclerView recyclerView = ((FastScrollRecyclerView)mRecyclerView);
            recyclerView.setPopupBgColor(Tool.getHeavyColor());
            recyclerView.setThumbColor(Tool.getHeavyColor());
        }
        mAdapter.notifyOnMediaStateChanged(AbsMediaAdapter.PALETTE_CHANGED);
        super.onPaletteChanged();
    }

    @Override
    public void onPlayStateChanged() {
        if(mAdapter!=null)mAdapter.notifyOnMediaStateChanged(AbsMediaAdapter.PLAY_STATE_CHANGED);
    }

    @Override
    public void onMediaStoreChanged() {
        ArrayList<Song> songs = SongLoader.getAllSongs(getActivity(),SortOrderBottomSheet.mSortOrderCodes[mCurrentSortOrder]);
        mAdapter.setData(songs);
        showOrHidePreview(!songs.isEmpty());
    }

    @Override
    public int getSavedOrder() {
        return mCurrentSortOrder;
    }

    @Override
    public void onOrderChanged(int newType, String name) {
        if(mCurrentSortOrder!=newType) {
            mCurrentSortOrder = newType;
            App.getInstance().getPreferencesUtility().setSongChildSortOrder(mCurrentSortOrder);
            refreshData();
        }
    }
}
