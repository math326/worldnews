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
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.worldnews.app.Constants;
import com.worldnews.app.R;
import com.worldnews.app.adapter.NewsAdapter;
import com.worldnews.app.api.RetrofitClient;
import com.worldnews.app.database.AppDatabase;
import com.worldnews.app.database.ArticleEntity;
import com.worldnews.app.databinding.FragmentNewsBinding;
import com.worldnews.app.model.Article;
import com.worldnews.app.model.NewsResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NewsFragment extends Fragment {

    private static final String ARG_CATEGORY = "category";

    private FragmentNewsBinding binding;
    private NewsAdapter adapter;
    private String category;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static NewsFragment newInstance(String category) {
        NewsFragment fragment = new NewsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CATEGORY, category);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            category = getArguments().getString(ARG_CATEGORY);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentNewsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new NewsAdapter(requireContext());
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerView.setAdapter(adapter);

        binding.swipeRefresh.setColorSchemeColors(
                requireContext().getColor(R.color.accent_red)
        );
        binding.swipeRefresh.setOnRefreshListener(this::loadNews);

        loadNews();
    }

    private void loadNews() {
        binding.swipeRefresh.setRefreshing(true);
        binding.progressBar.setVisibility(View.VISIBLE);

        if (!isNetworkAvailable()) {
            loadFromDatabase();
            return;
        }

        Call<NewsResponse> call;
        if (category.equals(Constants.CATEGORY_WORLD)) {
            call = RetrofitClient.getInstance().getNewsApiService()
                    .getTopHeadlines(null, "en", Constants.PAGE_SIZE, Constants.NEWS_API_KEY);
        } else if (category.equals(Constants.CATEGORY_TECH)) {
            call = RetrofitClient.getInstance().getNewsApiService()
                    .getTopHeadlines("technology", "en", Constants.PAGE_SIZE, Constants.NEWS_API_KEY);
        } else if (category.equals(Constants.CATEGORY_ECONOMY)) {
            call = RetrofitClient.getInstance().getNewsApiService()
                    .getTopHeadlines("business", "en", Constants.PAGE_SIZE, Constants.NEWS_API_KEY);
        } else if (category.equals(Constants.CATEGORY_POLITICS)) {
            call = RetrofitClient.getInstance().getNewsApiService()
                    .getEverything("politics world", "en", "publishedAt", Constants.PAGE_SIZE, Constants.NEWS_API_KEY);
        } else {
            call = RetrofitClient.getInstance().getNewsApiService()
                    .getTopHeadlines(null, "en", Constants.PAGE_SIZE, Constants.NEWS_API_KEY);
        }

        call.enqueue(new Callback<NewsResponse>() {
            @Override
            public void onResponse(@NonNull Call<NewsResponse> call,
                                   @NonNull Response<NewsResponse> response) {
                binding.swipeRefresh.setRefreshing(false);
                binding.progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    List<Article> articles = response.body().getArticles();
                    if (articles != null && !articles.isEmpty()) {
                        adapter.setArticles(articles);
                        binding.tvError.setVisibility(View.GONE);
                        saveToDatabase(articles);
                    } else {
                        showErrorOrOffline();
                    }
                } else {
                    showErrorOrOffline();
                }
            }

            @Override
            public void onFailure(@NonNull Call<NewsResponse> call, @NonNull Throwable t) {
                binding.swipeRefresh.setRefreshing(false);
                binding.progressBar.setVisibility(View.GONE);
                loadFromDatabase();
            }
        });
    }

    private void loadFromDatabase() {
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            List<ArticleEntity> entities = db.articleDao().getArticlesByCategory(category);
            List<Article> articles = convertEntitiesToArticles(entities);

            mainHandler.post(() -> {
                binding.swipeRefresh.setRefreshing(false);
                binding.progressBar.setVisibility(View.GONE);
                if (!articles.isEmpty()) {
                    adapter.setArticles(articles);
                    binding.tvError.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Modo offline — mostrando notícias salvas", Toast.LENGTH_SHORT).show();
                } else {
                    showErrorOrOffline();
                }
            });
        });
    }

    private void saveToDatabase(List<Article> articles) {
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            db.articleDao().deleteArticlesByCategory(category);
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
                            category
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

    private void showErrorOrOffline() {
        binding.tvError.setVisibility(View.VISIBLE);
        binding.tvError.setText("Sem conexão com internet.\nPuxe para baixo para tentar novamente.");
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) requireContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
