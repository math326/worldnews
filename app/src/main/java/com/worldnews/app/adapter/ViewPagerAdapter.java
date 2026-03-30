package com.worldnews.app.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.worldnews.app.Constants;
import com.worldnews.app.fragment.LocalNewsFragment;
import com.worldnews.app.fragment.NewsFragment;
import com.worldnews.app.fragment.RealTimeFragment;
import java.util.ArrayList;
import java.util.List;

public class ViewPagerAdapter extends FragmentStateAdapter {

    private final List<Fragment> fragments = new ArrayList<>();
    private final List<String> titles = new ArrayList<>();

    public ViewPagerAdapter(@NonNull FragmentManager fm, @NonNull Lifecycle lifecycle) {
        super(fm, lifecycle);
        setupFragments();
    }

    private void setupFragments() {
        fragments.add(NewsFragment.newInstance(Constants.CATEGORY_WORLD));
        titles.add("Mundo");

        fragments.add(RealTimeFragment.newInstance());
        titles.add("Ao Vivo");

        fragments.add(NewsFragment.newInstance(Constants.CATEGORY_POLITICS));
        titles.add("Política");

        fragments.add(NewsFragment.newInstance(Constants.CATEGORY_TECH));
        titles.add("Tecnologia");

        fragments.add(NewsFragment.newInstance(Constants.CATEGORY_ECONOMY));
        titles.add("Economia");

        fragments.add(LocalNewsFragment.newInstance());
        titles.add("Meu Estado");
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return fragments.get(position);
    }

    @Override
    public int getItemCount() {
        return fragments.size();
    }

    public String getPageTitle(int position) {
        return titles.get(position);
    }
}
