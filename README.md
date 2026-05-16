# WorldNews

Aplicativo Android (Java) para leitura de notícias que consome a NewsAPI, feeds RSS e oferece tradução via MyMemory API. Fornece abas por categoria, um feed "Ao Vivo" e notícias locais com cache offline via Room.

## Funcionalidades
- Exibe **Top Headlines** por categoria (Mundo, Política, Tecnologia, Economia, etc.).
- Feed **Ao Vivo** com atualização periódica e indicadores de urgência.
- Aba **Meu Estado**: busca localização, tenta RSS local ou consulta por estado.
- Tradução automática de títulos/descrições para o idioma do dispositivo (quando necessário).
- Cache offline usando Room.

## Requisitos
- Android Studio ou Gradle
- Android SDK: minSdk 24, target/compileSdk 36
- Permissões: `INTERNET` e (opcional) `ACCESS_COARSE_LOCATION` / `ACCESS_FINE_LOCATION`

## Instalação e execução
1. Clone o repositório.
2. Abra o projeto no Android Studio ou use Gradle via linha de comando:

```bash
./gradlew assembleDebug
./gradlew installDebug
```

3. Configure as chaves de API (NewsAPI e MyMemory) em `Constants.java` ou via `build.gradle`/`gradle.properties` conforme a configuração do projeto.
4. Execute em um emulador ou dispositivo Android (API >= 24). Conceda permissão de localização para a aba "Meu Estado" quando solicitado.

## Configuração de API keys
Coloque as chaves onde o projeto espera (ver `Constants.java`). É recomendado não commitar chaves diretamente — use `gradle.properties` ou variáveis de ambiente e injete via `BuildConfig`.

## Estrutura do projeto (resumo)

app/
- `build.gradle` — configuração do módulo app.
- `src/main/java/com/...` — código fonte Java:
  - `MainActivity.java` — Activity principal: configura Toolbar, `ViewPager2` e `TabLayout`.
  - `SplashActivity.java` — Tela de abertura com animação.
  - `fragment/` — Fragments por aba:
    - `NewsFragment.java` — Fragment genérico para categorias; carrega e salva notícias.
    - `RealTimeFragment.java` — Aba "Ao Vivo"; atualiza automaticamente.
    - `LocalNewsFragment.java` — Aba "Meu Estado"; resolve localização e carrega RSS ou query.
  - `adapter/` — Adapters para RecyclerView e ViewPager:
    - `NewsAdapter.java` — Adapter dos cards de notícia; integra Glide e tradução.
    - `RealTimeAdapter.java` — Adapter do feed ao vivo com indicadores de urgência.
    - `ViewPagerAdapter.java` — Gerencia os fragments do `ViewPager2`.
  - `api/` — Camada de rede:
    - `NewsApiService.java` — Interface Retrofit para NewsAPI.
    - `TranslateApiService.java` — Interface Retrofit para MyMemory.
    - `RetrofitClient.java` — Singleton Retrofit com OkHttp.
    - `RssParser.java` — Parser RSS com `HttpURLConnection` + `XmlPullParser`.
  - `database/` — Room (cache):
    - `AppDatabase.java` — instancia o banco Room.
    - `ArticleDao.java` — queries (insert, select por categoria, delete, etc.).
    - `ArticleEntity.java` — entidade `articles` (PK: url, campos: title, description, urlToImage, publishedAt, sourceName, category, savedAt).
  - `model/` — modelos de domínio:
    - `Article.java`, `NewsResponse.java`, `RssItem.java`, `TranslationResponse.java`.
  - `utils/` — utilitários:
    - `TranslationManager.java` — gerencia traduções com cache em memória.

- `src/main/res/` — layouts, drawables, animações (`res/anim/`), resources do app.

## Como o app funciona (fluxo)
1. `SplashActivity` abre e redireciona para `MainActivity`.
2. `MainActivity` monta o `ViewPager2` com 6 abas; cada aba é um `Fragment`.
3. `NewsFragment` chama a `NewsApiService` (Retrofit) para carregar notícias por categoria; salva no Room e exibe no `NewsAdapter`.
4. `RealTimeFragment` busca notícias com query específica de conflitos/geopolítica e se atualiza a cada 5 minutos via `Handler`.
5. `LocalNewsFragment` solicita permissão de localização; usa `FusedLocationProviderClient` + `Geocoder` para resolver o estado; tenta carregar RSS mapeado via `RssParser` ou consulta a NewsAPI por query do estado.
6. `TranslationManager` traduz textos via MyMemory quando o idioma do dispositivo não for inglês — mantém cache em memória para evitar chamadas repetidas.

## Bibliotecas principais
- Retrofit 2.9 + Gson Converter
- OkHttp Logging Interceptor
- Glide 4.x
- Room 2.6
- RecyclerView, CardView, SwipeRefreshLayout, ViewPager2
- Play Services Location (FusedLocationProviderClient)

## Boas práticas e melhorias sugeridas
- Não commitar chaves de API; use `gradle.properties` ou variáveis de ambiente.
- Persistir cache de traduções (Room/SharedPreferences) para reduzir chamadas de rede.
- Adicionar testes instrumentados e unitários para Fragments e componentes de rede.
- Adicionar CI (GitHub Actions) para builds e lint automáticos.

## Contribuindo
- Abra uma issue com descrição clara do bug/feature.
- Crie um branch, implemente, rode `./gradlew check` e envie um Pull Request com detalhes e screenshots.

## Licença
Adicione um `LICENSE` apropriado (MIT/Apache2) se desejar tornar o projeto público.

---
_Documentação gerada automaticamente por assistente. Para detalhes internos, consulte o guia do projeto (`claude.md`)._
