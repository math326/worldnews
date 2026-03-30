package com.worldnews.app.api;

import com.worldnews.app.model.NewsResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface NewsApiService {

    // Busca notícias principais (top headlines) por categoria
    @GET("top-headlines")
    Call<NewsResponse> getTopHeadlines(
            @Query("category") String category,
            @Query("language") String language,
            @Query("pageSize") int pageSize,
            @Query("apiKey") String apiKey
    );

    // Busca notícias do mundo todo por categoria (sem filtro de país)
    @GET("everything")
    Call<NewsResponse> getEverything(
            @Query("q") String query,
            @Query("language") String language,
            @Query("sortBy") String sortBy,
            @Query("pageSize") int pageSize,
            @Query("apiKey") String apiKey
    );

    // Busca notícias de tempo real (conflitos/geopolítica)
    @GET("everything")
    Call<NewsResponse> getRealTimeNews(
            @Query("q") String query,
            @Query("sortBy") String sortBy,
            @Query("pageSize") int pageSize,
            @Query("apiKey") String apiKey
    );

    // Busca top headlines sem filtro de país (mundo)
    @GET("top-headlines")
    Call<NewsResponse> getWorldHeadlines(
            @Query("sources") String sources,
            @Query("pageSize") int pageSize,
            @Query("apiKey") String apiKey
    );
}
