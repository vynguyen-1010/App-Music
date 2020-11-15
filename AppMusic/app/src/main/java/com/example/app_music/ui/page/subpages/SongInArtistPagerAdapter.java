package com.fhm.musicr.ui.page.subpages;

import android.content.Context;
import androidx.appcompat.app.AppCompatActivity;

import com.fhm.musicr.R;
import com.fhm.musicr.helper.menu.SongMenuHelper;
import com.fhm.musicr.ui.page.librarypage.song.SongChildAdapter;
import com.fhm.musicr.ui.bottomsheet.OptionBottomSheet;

public class SongInArtistPagerAdapter extends SongChildAdapter {
    private static final String TAG = "SongInArtistPagerAdapter";

    public SongInArtistPagerAdapter(Context context) {
        super(context);
    }

    @Override
    public int getItemViewType(int position) {
        if(position==0) return R.layout.item_sort_song_child;
        return R.layout.item_song_bigger;
    }

    @Override
    protected void onMenuItemClick(int positionInData) {
        OptionBottomSheet
                .newInstance(SongMenuHelper.SONG_ARTIST_OPTION,getData().get(positionInData))
                .show(((AppCompatActivity)mContext).getSupportFragmentManager(), "song_popup_menu");
    }
}
