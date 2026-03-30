package com.worldnews.app.utils;

import android.os.Handler;
import android.os.Looper;
import com.worldnews.app.api.TranslateApiService;
import com.worldnews.app.model.TranslationResponse;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class TranslationManager {

    private static final String MYMEMORY_BASE_URL = "https://api.mymemory.translated.net/";
    // MyMemory limita a 500 caracteres por chamada no plano gratuito
    private static final int MAX_CHARS = 500;

    private static TranslationManager instance;

    private final TranslateApiService apiService;
    private final ExecutorService executor;
    private final Handler mainHandler;
    private final Map<String, String> cache;
    private final String deviceLanguage;
    private final String langPair; // ex: "en|pt"

    public interface TranslationCallback {
        void onTranslated(String translatedText);
    }

    private TranslationManager() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(MYMEMORY_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(TranslateApiService.class);
        executor = Executors.newFixedThreadPool(4);
        mainHandler = new Handler(Looper.getMainLooper());
        cache = new HashMap<>();

        // Detecta idioma do dispositivo (ex: "pt", "es", "fr", "de"...)
        deviceLanguage = Locale.getDefault().getLanguage();
        langPair = "en|" + deviceLanguage;
    }

    public static synchronized TranslationManager getInstance() {
        if (instance == null) {
            instance = new TranslationManager();
        }
        return instance;
    }

    /**
     * Retorna true se tradução é necessária.
     * Notícias vêm em inglês — só traduz se o dispositivo NÃO estiver em inglês.
     */
    public boolean shouldTranslate() {
        return !deviceLanguage.startsWith("en");
    }

    /**
     * Traduz um texto de "en" para o idioma do dispositivo via MyMemory API.
     * - Se o idioma do dispositivo for inglês, devolve o texto original imediatamente.
     * - Trunca em 500 caracteres antes de enviar (limite da API gratuita).
     * - Usa cache em memória para não repetir chamadas idênticas.
     * - Em caso de qualquer falha, devolve o texto original sem quebrar o app.
     *
     * @param text     Texto em inglês para traduzir
     * @param callback Chamado no main thread com o resultado
     */
    public void translate(String text, TranslationCallback callback) {
        if (text == null || text.isEmpty() || !shouldTranslate()) {
            callback.onTranslated(text);
            return;
        }

        // Trunca se necessário para respeitar o limite da API gratuita
        final String textToSend = text.length() > MAX_CHARS ? text.substring(0, MAX_CHARS) : text;

        // Chave de cache baseada no hash do texto truncado + idioma alvo
        String cacheKey = deviceLanguage + ":" + textToSend.hashCode();
        synchronized (cache) {
            if (cache.containsKey(cacheKey)) {
                callback.onTranslated(cache.get(cacheKey));
                return;
            }
        }

        executor.execute(() -> {
            try {
                Call<TranslationResponse> call = apiService.translate(textToSend, langPair);
                Response<TranslationResponse> response = call.execute();

                if (response.isSuccessful()
                        && response.body() != null
                        && response.body().getResponseData() != null) {

                    String translated = response.body().getResponseData().getTranslatedText();

                    if (translated != null && !translated.isEmpty()) {
                        synchronized (cache) {
                            cache.put(cacheKey, translated);
                        }
                        mainHandler.post(() -> callback.onTranslated(translated));
                    } else {
                        mainHandler.post(() -> callback.onTranslated(text));
                    }
                } else {
                    mainHandler.post(() -> callback.onTranslated(text));
                }
            } catch (Exception e) {
                // Qualquer erro de rede ou parsing → mostra o texto original
                mainHandler.post(() -> callback.onTranslated(text));
            }
        });
    }
}
