package com.worldnews.app;

public class Constants {

    // Chave lida do BuildConfig (definida em local.properties, nunca no código-fonte)
    public static final String NEWS_API_KEY = BuildConfig.NEWS_API_KEY;

    public static final String BASE_URL = "https://newsapi.org/v2/";

    // Categorias de notícias
    public static final String CATEGORY_WORLD = "world";
    public static final String CATEGORY_POLITICS = "politics";
    public static final String CATEGORY_TECH = "technology";
    public static final String CATEGORY_ECONOMY = "business";
    public static final String CATEGORY_REALTIME = "realtime";

    // Queries para tempo real (conflitos e geopolítica)
    public static final String REALTIME_QUERY = "ukraine OR russia OR china OR war OR conflict OR military OR nato OR geopolitics";

    // Intervalo de atualização automática em tempo real (5 minutos em milissegundos)
    public static final long REALTIME_UPDATE_INTERVAL = 5 * 60 * 1000L;

    // Número máximo de artigos por requisição
    public static final int PAGE_SIZE = 30;
}
