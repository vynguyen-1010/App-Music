package com.fhm.musicr.ui.page.librarypage;

import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.motion.widget.MotionLayout;

import com.fhm.musicr.ui.MainActivity;
import com.fhm.musicr.ui.page.librarypage.artist.ArtistSearchEvent;
import com.fhm.musicr.ui.page.librarypage.song.SongSearchEvent;
import com.google.android.material.tabs.TabLayout;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.widget.SearchView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TabWidget;
import android.widget.Toast;

import com.fhm.musicr.R;
import com.fhm.musicr.ui.page.librarypage.artist.ArtistChildTab;
import com.fhm.musicr.ui.page.librarypage.playlist.PlaylistChildTab;
import com.fhm.musicr.ui.page.librarypage.song.SongChildTab;
import com.fhm.musicr.ui.widget.fragmentnavigationcontroller.SupportFragment;
import com.fhm.musicr.util.Tool;

import org.greenrobot.eventbus.EventBus;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.everything.android.ui.overscroll.adapters.ViewPagerOverScrollDecorAdapter;

public class LibraryTabFragment extends SupportFragment {
    private static final String TAG ="LibraryTabFragment";

    @BindView(R.id.back_image)
    ImageView mBackImage;
    @BindView(R.id.search_view)
    SearchView mSearchView;
    @BindView(R.id.tab_layout)
    TabLayout mTabLayout;
    @BindView(R.id.view_pager)
    ViewPager mViewPager;
    LibraryPagerAdapter mPagerAdapter;
    @BindView(R.id.status_bar) View mStatusView;
    @BindView(R.id.root)
    MotionLayout mMotionLayout;

    String songSearch = "";
    String artistSearch = "";

    @Nullable
    @Override
    protected View onCreateView(LayoutInflater inflater, ViewGroup container) {
        View view = inflater.inflate(R.layout.library_tab,container,false);

        return view;

    }

    @OnClick(R.id.search_view)
    void searchViewClicked() {
        mSearchView.onActionViewExpanded();
        mSearchView.setIconified(true);

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this,view);
        mStatusView.getLayoutParams().height = Tool.getStatusHeight(getResources());
        mStatusView.requestLayout();

        mViewPager.setOnTouchListener((v, event) -> getMainActivity().backStackStreamOnTouchEvent(event));
        mPagerAdapter = new LibraryPagerAdapter(getActivity(),getChildFragmentManager());
        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.setOffscreenPageLimit(5);
        mTabLayout.setupWithViewPager(mViewPager);
        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(mTabLayout));
        mTabLayout.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(mViewPager));

        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(mTabLayout));

        mTabLayout.addOnTabSelectedListener(new TabLayout.BaseOnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int currentItem = tab.getPosition();

                if(currentItem == 0){
                    mSearchView.setQuery(""+songSearch, true);
                } else if(currentItem == 1){
                    mSearchView.setQuery("", false);
                } else if(currentItem == 2){
                    mSearchView.setQuery(""+artistSearch, true);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();

        mSearchView.onActionViewExpanded();
        mSearchView.clearFocus();
        mSearchView.setSubmitButtonEnabled(false);
        mSearchView.setQueryRefinementEnabled(false);
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // Bat su khien khi nhan hinh tim kiem tren ban phim
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Bat su kien khi dang nhap lieu tren khung search
                // Moi lan du lieu trong o search thay doi thi gui noi dung hien tai qua SongChildTab để load lai giao dien


                int currentItem = mViewPager.getCurrentItem();

                // currentItem = 0 => Song
                // currentItem = 2 => Artist
                if(currentItem == 0){
                    // Dung thu vien EventBus de gui du lieu sang Fragment SongChildTab
                    songSearch = newText;
                    EventBus.getDefault().post(new SongSearchEvent(newText));
                } else if(currentItem == 2){
                    artistSearch = newText;
                    EventBus.getDefault().post(new ArtistSearchEvent(newText));
                }

                return false;
            }
        });
    }

    public Fragment navigateToTab(int item) {
        if(item<mPagerAdapter.getCount()) {
            mViewPager.setCurrentItem(item, false);
           return mPagerAdapter.getItem(item);
        }
        return null;
    }

    public Fragment navigateToTab(final String tag) {
        switch (tag) {
            case SongChildTab.TAG:
                 return navigateToTab(0);
            case PlaylistChildTab.TAG:
                return navigateToTab(1);
            case ArtistChildTab.TAG:
                return navigateToTab(2);
             default:
                 return null;
        }
    }
}
