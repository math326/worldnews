package com.worldnews.app.api;

import com.worldnews.app.model.TranslationResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface TranslateApiService {

    // MyMemory API — gratuita, sem chave necessária
    // GET https://api.mymemory.translated.net/get?q={texto}&langpair=en|pt
    @GET("get")
    Call<TranslationResponse> translate(
            @Query("q") String text,
            @Query("langpair") String langPair
    );
}
