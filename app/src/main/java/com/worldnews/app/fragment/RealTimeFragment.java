package com.worldnews.app.fragment;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import com.worldnews.app.Constants;
import com.worldnews.app.R;
import com.worldnews.app.adapter.RealTimeAdapter;
import com.worldnews.app.api.RetrofitClient;
import com.worldnews.app.database.AppDatabase;
import com.worldnews.app.database.ArticleEntity;
import com.worldnews.app.databinding.FragmentRealtimeBinding;
import com.worldnews.app.model.Article;
import com.worldnews.app.model.NewsResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RealTimeFragment extends Fragment {

    public static RealTimeFragment newInstance() {
        return new RealTimeFragment();
    }

    private FragmentRealtimeBinding binding;
    private RealTimeAdapter adapter;
    private final Handler autoRefreshHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final Runnable autoRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            loadRealTimeNews();
            autoRefreshHandler.postDelayed(this, Constants.REALTIME_UPDATE_INTERVAL);
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRealtimeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new RealTimeAdapter(requireContext());
        binding.recyclerViewRealtime.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewRealtime.setAdapter(adapter);

        binding.swipeRefreshRealtime.setColorSchemeColors(
                requireContext().getColor(R.color.accent_red)
        );
        binding.swipeRefreshRealtime.setOnRefreshListener(this::loadRealTimeNews);

        // Animação pulsante no ponto vermelho "AO VIVO"
        Animation pulseAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.pulse);
        binding.liveDot.startAnimation(pulseAnim);

        loadRealTimeNews();
        // Inicia auto-refresh a cada 5 minutos
        autoRefreshHandler.postDelayed(autoRefreshRunnable, Constants.REALTIME_UPDATE_INTERVAL);
    }

    private void loadRealTimeNews() {
        binding.swipeRefreshRealtime.setRefreshing(true);
        binding.progressBarRt.setVisibility(View.VISIBLE);

        if (!isNetworkAvailable()) {
            loadFromDatabase();
            return;
        }

        RetrofitClient.getInstance().getNewsApiService()
                .getRealTimeNews(
                        Constants.REALTIME_QUERY,
                        "publishedAt",
                        Constants.PAGE_SIZE,
                        Constants.NEWS_API_KEY
                )
                .enqueue(new Callback<NewsResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<NewsResponse> call,
                                           @NonNull Response<NewsResponse> response) {
                        binding.swipeRefreshRealtime.setRefreshing(false);
                        binding.progressBarRt.setVisibility(View.GONE);

                        if (response.isSuccessful() && response.body() != null) {
                            List<Article> articles = response.body().getArticles();
                            if (articles != null && !articles.isEmpty()) {
                                adapter.setArticles(articles);
                                binding.tvErrorRt.setVisibility(View.GONE);
                                saveToDatabase(articles);
                                updateLastRefreshTime();
                            } else {
                                loadFromDatabase();
                            }
                        } else {
                            loadFromDatabase();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<NewsResponse> call, @NonNull Throwable t) {
                        binding.swipeRefreshRealtime.setRefreshing(false);
                        binding.progressBarRt.setVisibility(View.GONE);
                        loadFromDatabase();
                    }
                });
    }

    private void updateLastRefreshTime() {
        binding.tvLastUpdate.setVisibility(View.VISIBLE);
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(
                "HH:mm:ss", java.util.Locale.getDefault());
        String time = sdf.format(new java.util.Date());
        binding.tvLastUpdate.setText("Última atualização: " + time);
    }

    private void loadFromDatabase() {
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            List<ArticleEntity> entities = db.articleDao()
                    .getArticlesByCategory(Constants.CATEGORY_REALTIME);
            List<Article> articles = convertEntitiesToArticles(entities);

            mainHandler.post(() -> {
                binding.swipeRefreshRealtime.setRefreshing(false);
                binding.progressBarRt.setVisibility(View.GONE);
                if (!articles.isEmpty()) {
                    adapter.setArticles(articles);
                    binding.tvErrorRt.setVisibility(View.GONE);
                } else {
                    binding.tvErrorRt.setVisibility(View.VISIBLE);
                    binding.tvErrorRt.setText("Sem dados disponíveis.\nVerifique sua conexão.");
                }
            });
        });
    }

    private void saveToDatabase(List<Article> articles) {
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            db.articleDao().deleteArticlesByCategory(Constants.CATEGORY_REALTIME);
            List<ArticleEntity> entities = new ArrayList<>();
            for (Article a : articles) {
                if (a.getUrl() != null) {
                    entities.add(new ArticleEntity(
                            a.getUrl(),
                            a.getTitle(),
                            a.getDescription(),
                            a.getUrlToImage(),
                            a.getPublishedAt(),
                            a.getSource() != null ? a.getSource().getName() : "",
                            Constants.CATEGORY_REALTIME
                    ));
                }
            }
            db.articleDao().insertArticles(entities);
        });
    }

    private List<Article> convertEntitiesToArticles(List<ArticleEntity> entities) {
        List<Article> articles = new ArrayList<>();
        for (ArticleEntity e : entities) {
            Article a = new Article();
            a.setTitle(e.getTitle());
            a.setDescription(e.getDescription());
            a.setUrl(e.getUrl());
            a.setUrlToImage(e.getUrlToImage());
            a.setPublishedAt(e.getPublishedAt());
            Article.Source source = new Article.Source();
            source.setName(e.getSourceName());
            a.setSource(source);
            articles.add(a);
        }
        return articles;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) requireContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    @Override
    public void onResume() {
        super.onResume();
        autoRefreshHandler.removeCallbacks(autoRefreshRunnable);
        autoRefreshHandler.postDelayed(autoRefreshRunnable, Constants.REALTIME_UPDATE_INTERVAL);
    }

    @Override
    public void onPause() {
        super.onPause();
        autoRefreshHandler.removeCallbacks(autoRefreshRunnable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        autoRefreshHandler.removeCallbacks(autoRefreshRunnable);
        binding = null;
    }
}
