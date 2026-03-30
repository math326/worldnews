package com.worldnews.app;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.worldnews.app.adapter.ViewPagerAdapter;
import com.worldnews.app.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private ViewPagerAdapter viewPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager(), getLifecycle());
        binding.viewPager.setAdapter(viewPagerAdapter);

        // Conectar TabLayout ao ViewPager2
        new TabLayoutMediator(binding.tabLayout, binding.viewPager, (tab, position) -> {
            tab.setText(viewPagerAdapter.getPageTitle(position));
            // Ícone especial para "Ao Vivo"
            if (position == 1) {
                tab.setIcon(R.drawable.ic_live);
            }
        }).attach();

        // Desabilitar swipe para evitar conflito com SwipeRefreshLayout
        binding.viewPager.setUserInputEnabled(true);
        binding.viewPager.setOffscreenPageLimit(4);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
