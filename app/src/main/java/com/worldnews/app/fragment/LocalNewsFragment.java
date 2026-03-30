package com.worldnews.app.fragment;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.util.Log;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.worldnews.app.Constants;
import com.worldnews.app.R;
import com.worldnews.app.adapter.NewsAdapter;
import com.worldnews.app.api.RetrofitClient;
import com.worldnews.app.api.RssParser;
import com.worldnews.app.databinding.FragmentLocalNewsBinding;
import com.worldnews.app.model.Article;
import com.worldnews.app.model.NewsResponse;
import com.worldnews.app.model.RssItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LocalNewsFragment extends Fragment {

    private static final String TAG = "LocalNews";
    // Feed nacional de fallback (Folha UOL — nacional)
    private static final String RSS_NACIONAL = "https://feeds.folha.uol.com.br/emcimadahora/rss091.xml";

    private FragmentLocalNewsBinding binding;
    private NewsAdapter adapter;
    private FusedLocationProviderClient fusedLocationClient;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Estados com RSS próprio funcional; demais usam fallback NewsAPI
    private static final Map<String, String> STATE_RSS_MAP = new HashMap<>();
    static {
        STATE_RSS_MAP.put("São Paulo",         "https://feeds.folha.uol.com.br/cotidiano/rss091.xml");
        STATE_RSS_MAP.put("Rio de Janeiro",    "https://feeds.folha.uol.com.br/cotidiano/rss091.xml");
        STATE_RSS_MAP.put("Minas Gerais",      "https://www.otempo.com.br/rss");
        STATE_RSS_MAP.put("Bahia",             "https://feeds.feedburner.com/correio24horas");
        STATE_RSS_MAP.put("Paraná",            "https://www.gazetadopovo.com.br/rss/ultimas-noticias.xml");
        STATE_RSS_MAP.put("Rio Grande do Sul", "https://gauchazh.clicrbs.com.br/rss");
    }

    // Estados que usam fallback da NewsAPI (query pelo nome do estado)
    private static final Map<String, String> STATE_NEWSAPI_QUERY = new HashMap<>();
    static {
        STATE_NEWSAPI_QUERY.put("Pernambuco",          "pernambuco+noticias");
        STATE_NEWSAPI_QUERY.put("Ceará",               "ceara+noticias");
        STATE_NEWSAPI_QUERY.put("Pará",                "para+noticias+brasil");
        STATE_NEWSAPI_QUERY.put("Amazonas",            "amazonas+noticias");
        STATE_NEWSAPI_QUERY.put("Goiás",               "goias+noticias");
        STATE_NEWSAPI_QUERY.put("Maranhão",            "maranhao+noticias");
        STATE_NEWSAPI_QUERY.put("Santa Catarina",      "santa+catarina+noticias");
        STATE_NEWSAPI_QUERY.put("Mato Grosso",         "mato+grosso+noticias");
        STATE_NEWSAPI_QUERY.put("Espírito Santo",      "espirito+santo+noticias");
        STATE_NEWSAPI_QUERY.put("Alagoas",             "alagoas+noticias");
        STATE_NEWSAPI_QUERY.put("Piauí",               "piaui+noticias");
        STATE_NEWSAPI_QUERY.put("Rio Grande do Norte", "rio+grande+norte+noticias");
        STATE_NEWSAPI_QUERY.put("Paraíba",             "paraiba+noticias");
        STATE_NEWSAPI_QUERY.put("Sergipe",             "sergipe+noticias");
        STATE_NEWSAPI_QUERY.put("Rondônia",            "rondonia+noticias");
        STATE_NEWSAPI_QUERY.put("Tocantins",           "tocantins+noticias");
        STATE_NEWSAPI_QUERY.put("Acre",                "acre+noticias+brasil");
        STATE_NEWSAPI_QUERY.put("Amapá",               "amapa+noticias");
        STATE_NEWSAPI_QUERY.put("Roraima",             "roraima+noticias");
        STATE_NEWSAPI_QUERY.put("Mato Grosso do Sul",  "mato+grosso+sul+noticias");
        STATE_NEWSAPI_QUERY.put("Distrito Federal",    "distrito+federal+brasilia+noticias");
    }

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean coarse = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
                Boolean fine = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                if (Boolean.TRUE.equals(coarse) || Boolean.TRUE.equals(fine)) {
                    fetchLocation();
                } else {
                    // Permissão negada → nacional como fallback
                    loadViaRss("Brasil", RSS_NACIONAL);
                    Toast.makeText(requireContext(),
                            "Permissão de localização negada. Exibindo notícias nacionais.",
                            Toast.LENGTH_SHORT).show();
                }
            });

    public static LocalNewsFragment newInstance() {
        return new LocalNewsFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLocalNewsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        adapter = new NewsAdapter(requireContext());
        binding.recyclerViewLocal.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewLocal.setAdapter(adapter);

        binding.swipeRefreshLocal.setColorSchemeColors(
                requireContext().getColor(R.color.accent_red));
        binding.swipeRefreshLocal.setOnRefreshListener(this::requestLocationAndLoad);

        requestLocationAndLoad();
    }

    private void requestLocationAndLoad() {
        // Verifica se já tem permissão
        boolean hasCoarse = ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean hasFine = ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (hasCoarse || hasFine) {
            fetchLocation();
        } else {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
            });
        }
    }

    private void fetchLocation() {
        binding.progressBarLocal.setVisibility(View.VISIBLE);
        binding.tvErrorLocal.setVisibility(View.GONE);

        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            resolveStateFromLocation(location.getLatitude(), location.getLongitude());
                        } else {
                            // Sem localização recente → tenta última conhecida
                            fetchLastKnownLocation();
                        }
                    })
                    .addOnFailureListener(e -> loadViaRss("Brasil", RSS_NACIONAL));
        } catch (SecurityException e) {
            loadViaRss("Brasil", RSS_NACIONAL);
        }
    }

    private void fetchLastKnownLocation() {
        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            resolveStateFromLocation(location.getLatitude(), location.getLongitude());
                        } else {
                            loadViaRss("Brasil", RSS_NACIONAL);
                        }
                    })
                    .addOnFailureListener(e -> loadViaRss("Brasil", RSS_NACIONAL));
        } catch (SecurityException e) {
            loadViaRss("Brasil", RSS_NACIONAL);
        }
    }

    private void resolveStateFromLocation(double lat, double lon) {
        executor.execute(() -> {
            try {
                Geocoder geocoder = new Geocoder(requireContext(), new Locale("pt", "BR"));
                List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);

                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    String countryCode = address.getCountryCode();

                    // Fora do Brasil → feed nacional
                    if (!"BR".equals(countryCode)) {
                        mainHandler.post(() -> loadViaRss("Brasil", RSS_NACIONAL));
                        return;
                    }

                    String adminArea = address.getAdminArea(); // nome do estado
                    mainHandler.post(() -> dispatchStateLoad(adminArea));
                } else {
                    mainHandler.post(() -> loadViaRss("Brasil", RSS_NACIONAL));
                }
            } catch (Exception e) {
                mainHandler.post(() -> loadViaRss("Brasil", RSS_NACIONAL));
            }
        });
    }

    /**
     * Resolve o estado detectado para uma fonte de notícias.
     * Prioridade: RSS próprio → NewsAPI por estado → RSS nacional.
     */
    private void dispatchStateLoad(String stateName) {
        Log.d(TAG, "Estado detectado: " + stateName);

        // 1. Tem RSS próprio?
        String rssUrl = findInMap(STATE_RSS_MAP, stateName);
        if (rssUrl != null) {
            Log.d(TAG, "URL RSS: " + rssUrl);
            loadViaRss(stateName, rssUrl);
            return;
        }

        // 2. Tem query NewsAPI mapeada?
        String query = findInMap(STATE_NEWSAPI_QUERY, stateName);
        if (query != null) {
            Log.d(TAG, "URL RSS: NewsAPI query=" + query);
            loadViaNewsApi(stateName, query);
            return;
        }

        // 3. Fallback: RSS nacional
        Log.d(TAG, "URL RSS: " + RSS_NACIONAL + " (fallback nacional)");
        loadViaRss("Brasil", RSS_NACIONAL);
    }

    /** Busca exata, depois parcial case-insensitive para lidar com variações do Geocoder. */
    private <V> V findInMap(Map<String, V> map, String stateName) {
        if (stateName == null) return null;
        if (map.containsKey(stateName)) return map.get(stateName);
        String lower = stateName.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, V> entry : map.entrySet()) {
            String keyLower = entry.getKey().toLowerCase(Locale.ROOT);
            if (keyLower.contains(lower) || lower.contains(keyLower)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private void loadViaRss(String stateName, String rssUrl) {
        binding.tvStateLabel.setText("Notícias de " + stateName);
        binding.swipeRefreshLocal.setRefreshing(true);
        binding.progressBarLocal.setVisibility(View.VISIBLE);
        binding.tvErrorLocal.setVisibility(View.GONE);

        executor.execute(() -> {
            try {
                RssParser parser = new RssParser();
                List<RssItem> items = parser.parse(rssUrl);
                List<Article> articles = new ArrayList<>();
                for (RssItem item : items) {
                    articles.add(item.toArticle());
                }
                Log.d(TAG, "Artigos carregados: " + articles.size());

                mainHandler.post(() -> {
                    binding.swipeRefreshLocal.setRefreshing(false);
                    binding.progressBarLocal.setVisibility(View.GONE);
                    if (!articles.isEmpty()) {
                        adapter.setArticles(articles);
                        binding.tvErrorLocal.setVisibility(View.GONE);
                    } else {
                        // RSS retornou vazio — tenta fallback nacional
                        loadViaRss("Brasil", RSS_NACIONAL);
                    }
                });
            } catch (Exception e) {
                Log.d(TAG, "Erro no RSS: " + e.getMessage());
                mainHandler.post(() -> {
                    binding.swipeRefreshLocal.setRefreshing(false);
                    binding.progressBarLocal.setVisibility(View.GONE);
                    binding.tvErrorLocal.setVisibility(View.VISIBLE);
                    binding.tvErrorLocal.setText("Erro ao carregar notícias.\nVerifique sua conexão.");
                });
            }
        });
    }

    private void loadViaNewsApi(String stateName, String query) {
        binding.tvStateLabel.setText("Notícias de " + stateName);
        binding.swipeRefreshLocal.setRefreshing(true);
        binding.progressBarLocal.setVisibility(View.VISIBLE);
        binding.tvErrorLocal.setVisibility(View.GONE);

        RetrofitClient.getInstance().getNewsApiService()
                .getEverything(query, "pt", "publishedAt", Constants.PAGE_SIZE, Constants.NEWS_API_KEY)
                .enqueue(new Callback<NewsResponse>() {
                    @Override
                    public void onResponse(Call<NewsResponse> call, Response<NewsResponse> response) {
                        List<Article> articles = new ArrayList<>();
                        if (response.isSuccessful() && response.body() != null
                                && response.body().getArticles() != null) {
                            articles = response.body().getArticles();
                        }
                        Log.d(TAG, "Artigos carregados: " + articles.size());

                        binding.swipeRefreshLocal.setRefreshing(false);
                        binding.progressBarLocal.setVisibility(View.GONE);

                        if (!articles.isEmpty()) {
                            adapter.setArticles(articles);
                            binding.tvErrorLocal.setVisibility(View.GONE);
                        } else {
                            binding.tvErrorLocal.setVisibility(View.VISIBLE);
                            binding.tvErrorLocal.setText("Nenhuma notícia encontrada.\nPuxe para tentar novamente.");
                        }
                    }

                    @Override
                    public void onFailure(Call<NewsResponse> call, Throwable t) {
                        Log.d(TAG, "Erro NewsAPI: " + t.getMessage());
                        binding.swipeRefreshLocal.setRefreshing(false);
                        binding.progressBarLocal.setVisibility(View.GONE);
                        binding.tvErrorLocal.setVisibility(View.VISIBLE);
                        binding.tvErrorLocal.setText("Erro ao carregar notícias.\nVerifique sua conexão.");
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
