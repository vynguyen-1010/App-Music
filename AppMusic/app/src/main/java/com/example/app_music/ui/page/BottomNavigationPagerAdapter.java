package com.fhm.musicr.ui.page;

import android.content.Context;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.fhm.musicr.R;
import com.fhm.musicr.ui.page.featurepage.FeatureTabFragment;
import com.fhm.musicr.ui.page.librarypage.LibraryTabFragment;
import com.fhm.musicr.ui.page.settingpage.SettingTabFragment;
import com.fhm.musicr.ui.widget.navigate.NavigateFragment;

import java.util.ArrayList;

public class BottomNavigationPagerAdapter extends FragmentPagerAdapter {
    private Context mContext;

    public BottomNavigationPagerAdapter(Context context, FragmentManager fragmentManager) {
        super(fragmentManager);
        mContext = context;
        initData();
    }

    public ArrayList<NavigateFragment> mData = new ArrayList<>();

    public boolean onBackPressed(int position) {
        if(position<mData.size())
        return mData.get(position).onBackPressed();
        return false;
    }


    private void initData() {
        mData.add(NavigateFragment.newInstance(new FeatureTabFragment()));
        mData.add(NavigateFragment.newInstance(new LibraryTabFragment()));
        mData.add(NavigateFragment.newInstance(new SettingTabFragment()));
    }

    // Returns total number of pages
    @Override
    public int getCount() {
        return mData.size();
    }

    // Returns the fragment to display for that page
    @Override
    public Fragment getItem(int position) {
        if(position>=mData.size()) return null;
        return mData.get(position);
    }

    // Returns the page title for the top indicator
    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0: return mContext.getResources().getString(R.string.Suggest);
            case 1: return mContext.getResources().getString(R.string.library);
            case 2: return mContext.getResources().getString(R.string.settings);
            default:return null;
        }
    }
}
