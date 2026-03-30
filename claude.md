# WorldNews — Guia de Estudo do Projeto

---

## 1. Framework

**Android SDK** — framework nativo para apps Android em Java.
- `minSdk 24` → roda em Android 7.0+
- `targetSdk / compileSdk 36` → compila contra Android 15
- `ViewBinding` → gera classes Java a partir dos XMLs de layout, evita `findViewById`
- `BuildConfig` → injeta variáveis do `build.gradle` no código Java em tempo de compilação

---

## 2. Bibliotecas

| Biblioteca | Para que serve no projeto |
|---|---|
| **Retrofit 2.9** | Faz chamadas HTTP para a NewsAPI e MyMemory |
| **Gson Converter** | Converte JSON da API em objetos Java automaticamente |
| **OkHttp Logging** | Loga no Logcat as requisições HTTP em debug |
| **Glide 4.16** | Carrega e cacheia imagens das notícias nos cards |
| **Room 2.6** | Banco de dados local SQLite com mapeamento por anotações |
| **RecyclerView** | Lista rolável de cards de notícias |
| **CardView** | Estilo de card com cantos arredondados e sombra |
| **SwipeRefreshLayout** | Gesto de puxar para atualizar as listas |
| **ViewPager2** | Deslizar entre abas (Mundo, Ao Vivo, Política...) |
| **Material Components** | TabLayout, Toolbar e temas visuais do Material Design |
| **Fragment** | Cada aba é um Fragment independente |
| **Play Services Location 21** | FusedLocationProviderClient para obter GPS na aba Meu Estado |
| **XmlPullParser** | Parser XML nativo do Android, usado para ler RSS sem bibliotecas externas |

---

## 3. Classes — o que cada arquivo faz

### Raiz do app
| Arquivo | O que faz |
|---|---|
| `Constants.java` | Guarda todas as constantes globais: API key, URL base, categorias, query de tempo real, intervalo de atualização e tamanho de página |
| `MainActivity.java` | Tela principal: monta a Toolbar, o ViewPager2 e o TabLayout com as 6 abas |
| `SplashActivity.java` | Tela de abertura: exibe logo com animação fade+scale por 2 segundos e abre a MainActivity |

### model/
| Arquivo | O que faz |
|---|---|
| `Article.java` | Representa uma notícia da NewsAPI — campos: source, author, title, description, url, urlToImage, publishedAt, content |
| `NewsResponse.java` | Envelope da resposta da NewsAPI — contém status, totalResults e lista de Article |
| `RssItem.java` | Representa um item de feed RSS — campos: title, description, link, pubDate, imageUrl; converte para Article via `toArticle()` |
| `TranslationResponse.java` | Envelope da resposta da MyMemory API — contém o objeto `responseData` com o campo `translatedText` |

### api/
| Arquivo | O que faz |
|---|---|
| `NewsApiService.java` | Interface Retrofit com os 4 endpoints da NewsAPI: top-headlines por categoria, everything por query, tempo real e world headlines |
| `RetrofitClient.java` | Singleton que cria e fornece a instância do Retrofit para a NewsAPI com OkHttp e timeout de 30s |
| `TranslateApiService.java` | Interface Retrofit com o endpoint GET da MyMemory API para tradução de texto |
| `RssParser.java` | Consome e parseia feeds RSS via HttpURLConnection + XmlPullParser, extrai itens com título, descrição, link, data e imagem |

### database/
| Arquivo | O que faz |
|---|---|
| `AppDatabase.java` | Singleton do Room Database — cria e fornece o banco `worldnews_db` |
| `ArticleDao.java` | Interface com as queries SQL do Room: insert, select por categoria, delete por categoria, delete por data antiga e count |
| `ArticleEntity.java` | Tabela `articles` no banco — colunas: url (PK), title, description, urlToImage, publishedAt, sourceName, category, savedAt |

### adapter/
| Arquivo | O que faz |
|---|---|
| `NewsAdapter.java` | RecyclerView adapter para cards de notícia — carrega imagem com Glide, aciona tradução com TranslationManager, abre URL no browser ao clicar, compartilha via Intent no botão share |
| `RealTimeAdapter.java` | RecyclerView adapter para o feed ao vivo — exibe indicador colorido de urgência (verde/amarelo/cinza) com animação pulsante nos itens recentes, traduz títulos |
| `ViewPagerAdapter.java` | FragmentStateAdapter que registra os 6 fragments e seus títulos de aba: Mundo, Ao Vivo, Política, Tecnologia, Economia, Meu Estado |

### fragment/
| Arquivo | O que faz |
|---|---|
| `NewsFragment.java` | Fragment genérico de categoria — busca notícias na NewsAPI pelo category/query, salva no Room, carrega do Room offline, exibe no NewsAdapter |
| `RealTimeFragment.java` | Fragment da aba "Ao Vivo" — busca notícias de conflitos/geopolítica, atualiza automaticamente a cada 5 minutos via Handler, anima o ponto vermelho com pulse.xml |
| `LocalNewsFragment.java` | Fragment da aba "Meu Estado" — pede permissão de localização, usa FusedLocation + Geocoder para descobrir o estado, carrega RSS regional ou NewsAPI como fallback |

### utils/
| Arquivo | O que faz |
|---|---|
| `TranslationManager.java` | Singleton que traduz textos de inglês para o idioma do dispositivo via MyMemory API, com cache em HashMap, truncamento em 500 chars e 4 threads em paralelo |

---

## 4. Métodos principais

| Método | Onde | O que faz |
|---|---|---|
| `onCreate()` | MainActivity, SplashActivity | Ponto de entrada de cada Activity — infla o layout, configura as views |
| `onViewCreated()` | Todos os Fragments | Chamado após o layout do fragment estar pronto — configura RecyclerView, SwipeRefresh e dispara carregamento |
| `loadNews()` | NewsFragment | Verifica conexão → chama NewsAPI ou carrega do Room |
| `loadRealTimeNews()` | RealTimeFragment | Chama NewsAPI com query de conflitos → salva no Room → atualiza adapter |
| `dispatchStateLoad()` | LocalNewsFragment | Decide a fonte: RSS mapeado, NewsAPI por query ou fallback nacional |
| `loadViaRss()` | LocalNewsFragment | Chama RssParser em background, converte RssItem para Article, exibe no adapter |
| `loadViaNewsApi()` | LocalNewsFragment | Chama `getEverything()` com query do estado, exibe resultado no adapter |
| `bind()` | NewsAdapter, RealTimeAdapter | Preenche cada card com título, imagem, fonte, tempo relativo e aciona tradução |
| `translate()` | TranslationManager | Verifica cache → trunca texto → chama MyMemory em thread → devolve no main thread via callback |
| `parse()` | RssParser | Abre HttpURLConnection, parseia XML com XmlPullParser, retorna lista de RssItem |
| `toArticle()` | RssItem | Converte o item RSS para o objeto Article para reutilizar o NewsAdapter |
| `getRelativeTime()` | NewsAdapter, RealTimeAdapter | Converte ISO 8601 para texto relativo: "há 5 min", "há 2h", "ontem" |
| `saveToDatabase()` | NewsFragment, RealTimeFragment | Salva a lista de Article no Room em background thread |
| `loadFromDatabase()` | NewsFragment, RealTimeFragment | Lê artigos salvos do Room e exibe como fallback offline |
| `getInstance()` | RetrofitClient, AppDatabase, TranslationManager | Padrão Singleton — retorna a instância única da classe |
| `resolveStateFromLocation()` | LocalNewsFragment | Executa geocoding reverso em background para descobrir estado a partir de lat/lon |
| `fetchLocation()` | LocalNewsFragment | Solicita localização atual via FusedLocationProviderClient |

---

## 5. Tipos de variáveis usados

| Tipo | Onde aparece | Para que serve |
|---|---|---|
| `String` | Em todo o projeto | Textos: título, URL, idioma, categoria, query |
| `int` | NewsResponse, ArticleDao, Constants | totalResults, PAGE_SIZE, contagens |
| `long` | ArticleEntity, Constants, adapters | savedAt (timestamp), REALTIME_UPDATE_INTERVAL, diff de tempo |
| `boolean` | NewsFragment, LocalNewsFragment | hasCoarse, hasFine, shouldTranslate |
| `float` | SplashActivity | Valores de animação: fromXScale=0.8f, toXScale=1.0f |
| `List<Article>` | Fragments, Adapters | Lista de notícias passada para o RecyclerView |
| `List<RssItem>` | LocalNewsFragment, RssParser | Lista de itens RSS antes de converter para Article |
| `Map<String, String>` | LocalNewsFragment, TranslationManager | STATE_RSS_MAP, STATE_NEWSAPI_QUERY, cache de traduções |
| `Handler` | Fragments, TranslationManager | Posta callbacks de volta para o main thread |
| `ExecutorService` | Fragments, TranslationManager | Pool de threads para operações em background |
| `Call<T>` | Retrofit interfaces | Representa uma requisição HTTP pendente |
| `Response<T>` | Callbacks Retrofit | Encapsula a resposta HTTP com código e body |
| `ActivityMainBinding` | MainActivity | Classe gerada pelo ViewBinding para activity_main.xml |
| `FragmentNewsBinding` | NewsFragment | Classe gerada pelo ViewBinding para fragment_news.xml |
| `volatile` | AppDatabase | Garante visibilidade da instância entre threads |
| `static final` | Constants | Constantes imutáveis acessíveis sem instanciar a classe |

---

## 6. Extends e Implements

| Classe | Extends / Implements | Por quê |
|---|---|---|
| `MainActivity` | `extends AppCompatActivity` | É uma Activity com suporte à ActionBar |
| `SplashActivity` | `extends AppCompatActivity` | É uma Activity com suporte à ActionBar |
| `NewsFragment` | `extends Fragment` | É um Fragment gerenciado pelo ViewPager2 |
| `RealTimeFragment` | `extends Fragment` | É um Fragment gerenciado pelo ViewPager2 |
| `LocalNewsFragment` | `extends Fragment` | É um Fragment gerenciado pelo ViewPager2 |
| `NewsAdapter` | `extends RecyclerView.Adapter<NewsViewHolder>` | Precisa implementar os métodos do RecyclerView |
| `RealTimeAdapter` | `extends RecyclerView.Adapter<RealTimeViewHolder>` | Precisa implementar os métodos do RecyclerView |
| `ViewPagerAdapter` | `extends FragmentStateAdapter` | Gerencia ciclo de vida dos Fragments no ViewPager2 |
| `AppDatabase` | `extends RoomDatabase` | É o banco Room — classe abstrata que o Room implementa em tempo de compilação |
| `ArticleDao` | `interface` | Room gera a implementação SQL a partir das anotações |
| `NewsApiService` | `interface` | Retrofit gera a implementação HTTP a partir das anotações |
| `TranslateApiService` | `interface` | Retrofit gera a implementação HTTP a partir das anotações |
| `NewsViewHolder` | `extends RecyclerView.ViewHolder` | Guarda referências das views de cada card |
| `RealTimeViewHolder` | `extends RecyclerView.ViewHolder` | Guarda referências das views de cada item ao vivo |
| `TranslationCallback` | `interface` (interna) | Contrato do callback de tradução assíncrona |

---

## 7. Anotações

| Anotação | Biblioteca | Onde é usada | O que faz |
|---|---|---|---|
| `@SerializedName("campo")` | Gson | Article, NewsResponse, TranslationResponse | Mapeia o campo Java para a chave JSON correspondente |
| `@Entity(tableName = "articles")` | Room | ArticleEntity | Marca a classe como tabela do banco de dados |
| `@PrimaryKey` | Room | ArticleEntity | Define `url` como chave primária da tabela |
| `@NonNull` | AndroidX | ArticleEntity, métodos | Indica que o valor nunca pode ser nulo |
| `@Database(entities = {...})` | Room | AppDatabase | Declara quais entidades fazem parte do banco |
| `@Dao` | Room | ArticleDao | Marca a interface como Data Access Object do Room |
| `@Insert(onConflict = REPLACE)` | Room | ArticleDao | Insere registro, substituindo se já existir mesma PK |
| `@Query("SELECT ...")` | Room | ArticleDao | Executa SQL customizado |
| `@GET("caminho")` | Retrofit | NewsApiService, TranslateApiService | Define endpoint HTTP GET |
| `@POST("caminho")` | Retrofit | (removido — era LibreTranslate) | Definiria endpoint HTTP POST |
| `@Query("param")` | Retrofit | NewsApiService, TranslateApiService | Mapeia parâmetro Java para query string da URL |
| `@NonNull` | AndroidX | Adapters, ViewHolders | Indica parâmetros que não aceitam nulo |
| `@Nullable` | AndroidX | Fragments (onCreateView) | Indica que o retorno pode ser nulo |
| `@Override` | Java | Em todo o projeto | Indica que o método sobrescreve o da classe pai |

---

## 8. Funções

### Funções nativas do Java usadas
| Função | Onde | O que faz |
|---|---|---|
| `Locale.getDefault().getLanguage()` | TranslationManager | Retorna o código do idioma do dispositivo: "pt", "en", "es"... |
| `System.currentTimeMillis()` | ArticleEntity, adapters | Retorna o timestamp atual em milissegundos |
| `TimeUnit.MILLISECONDS.toMinutes/toHours/toDays()` | Adapters | Converte milissegundos para minutos/horas/dias |
| `text.substring(0, 500)` | TranslationManager | Trunca o texto para o limite da API gratuita |
| `text.hashCode()` | TranslationManager | Gera chave numérica para o cache |
| `String.replaceAll()` | RssParser.stripHtml() | Remove tags HTML da descrição do RSS via regex |
| `String.replace()` | RssParser.stripHtml() | Substitui entidades HTML (&amp; &lt; etc.) |
| `url.openConnection()` | RssParser | Abre conexão HTTP para baixar o feed RSS |
| `parser.next()` | RssParser | Avança para o próximo evento do XML |
| `handler.postDelayed()` | SplashActivity, RealTimeFragment | Executa código após um delay em milissegundos |
| `executor.execute()` | Vários | Envia tarefa para execução em background thread |
| `mainHandler.post()` | Vários | Posta código para execução no main thread (UI) |
| `synchronized(cache)` | TranslationManager | Trava o cache para acesso seguro em multithread |

### Funções do Android usadas
| Função | Onde | O que faz |
|---|---|---|
| `startActivity(intent)` | Adapters, SplashActivity | Abre outra Activity ou app externo (browser) |
| `Intent.ACTION_VIEW` | NewsAdapter, RealTimeAdapter | Abre a URL da notícia no browser padrão |
| `Intent.ACTION_SEND` | NewsAdapter | Dispara o seletor de compartilhamento do sistema |
| `Intent.createChooser()` | NewsAdapter | Mostra o diálogo "Compartilhar via..." |
| `AnimationUtils.loadAnimation()` | RealTimeAdapter, RealTimeFragment | Carrega animação XML do diretório res/anim/ |
| `view.startAnimation()` | RealTimeFragment, RealTimeAdapter | Aplica a animação na view |
| `view.clearAnimation()` | RealTimeAdapter | Remove animação anterior antes de aplicar nova |
| `Glide.with(context).load(url)` | NewsAdapter | Carrega imagem da URL com crossfade e placeholder |
| `geocoder.getFromLocation()` | LocalNewsFragment | Geocoding reverso: converte lat/lon em endereço |
| `Log.d("TAG", "mensagem")` | LocalNewsFragment | Loga mensagem de debug no Logcat |

---

## 9. Decisões (if/else/switch)

| Condição | Onde | Decisão tomada |
|---|---|---|
| `!isNetworkAvailable()` | NewsFragment, RealTimeFragment | Sem internet → carrega do Room offline |
| `response.isSuccessful() && body != null` | Todos os Callbacks | Resposta OK → usa dados; falha → fallback |
| `articles.isEmpty()` | Fragments | Lista vazia → exibe mensagem de erro |
| `!deviceLanguage.startsWith("en")` | TranslationManager | Idioma ≠ inglês → aciona tradução; inglês → retorna original |
| `cache.containsKey(cacheKey)` | TranslationManager | Já traduzido → devolve do cache sem nova requisição |
| `text.length() > MAX_CHARS` | TranslationManager | Texto > 500 chars → trunca antes de enviar |
| `translated != null && !translated.isEmpty()` | TranslationManager | Tradução válida → usa; vazio → retorna original |
| `article.getUrl().equals(itemView.getTag())` | Adapters | ViewHolder ainda representa o mesmo artigo → aplica tradução; reciclado → ignora |
| `diffHours < 1` | RealTimeAdapter | < 1h → indicador verde + animação pulsante |
| `diffHours < 6` | RealTimeAdapter | < 6h → indicador amarelo |
| `else` | RealTimeAdapter | ≥ 6h → indicador cinza sem animação |
| `"BR".equals(countryCode)` | LocalNewsFragment | No Brasil → busca estado; fora → feed nacional |
| `STATE_RSS_MAP.containsKey(state)` | LocalNewsFragment | Estado tem RSS → usa RSS; senão → verifica NewsAPI query |
| `STATE_NEWSAPI_QUERY.containsKey(state)` | LocalNewsFragment | Estado tem query → usa NewsAPI; senão → fallback nacional |
| `location != null` | LocalNewsFragment | GPS retornou → resolve estado; nulo → tenta lastLocation |
| `hasCoarse || hasFine` | LocalNewsFragment | Permissão já concedida → busca localização diretamente |
| `connection.getResponseCode() != HTTP_OK` | RssParser | Servidor não retornou 200 → retorna lista vazia |
| `type.startsWith("image")` | RssParser | Enclosure é imagem → usa como thumbnail do card |
| `position == 1` | MainActivity | Aba 1 (Ao Vivo) → adiciona ícone de live na tab |

**switch** usado em `RssParser.parseFeed()`:
```
switch (eventType):
  START_TAG → verifica qual tag e popula o RssItem atual
  END_TAG   → se for </item>, finaliza e adiciona à lista
```

---

## 10. Loops

| Loop | Onde | O que faz |
|---|---|---|
| `while (eventType != END_DOCUMENT)` | RssParser | Percorre todos os eventos do XML até o fim do documento |
| `for (RssItem item : items)` | LocalNewsFragment | Converte cada RssItem para Article antes de passar ao adapter |
| `for (Article a : articles)` | NewsFragment, RealTimeFragment | Converte cada Article para ArticleEntity antes de salvar no Room |
| `for (Map.Entry<String, V> entry : map.entrySet())` | LocalNewsFragment.findInMap() | Busca parcial case-insensitive nos mapas de estado |
| `for (ArticleEntity e : entities)` | NewsFragment, RealTimeFragment | Converte ArticleEntity de volta para Article ao ler offline |
| Loop interno do Glide | NewsAdapter | Glide gerencia internamente o download e cache de imagens |
| Loop de callback do ViewPager | ViewPagerAdapter | FragmentStateAdapter itera os fragments ao navegar entre abas |
| Loop do Handler | RealTimeFragment | `postDelayed` reagenda a si mesmo a cada 5 min (loop de timer) |

---

## 11. Fluxo completo — o que acontece quando o app abre

**1.** Android lança `SplashActivity` (definida como LAUNCHER no Manifest)

**2.** `SplashActivity.onCreate()` infla `activity_splash.xml` — exibe a logo "W" vermelha centralizada

**3.** `AnimationSet` roda fade (0→1) + scale (0.8→1.0) durante 600ms

**4.** `Handler.postDelayed()` aguarda 2000ms e chama `startActivity(MainActivity)` + `finish()`

**5.** `MainActivity.onCreate()` infla `activity_main.xml` com Toolbar + TabLayout + ViewPager2

**6.** `ViewPagerAdapter` é criado com 6 fragments instanciados: Mundo, Ao Vivo, Política, Tecnologia, Economia, Meu Estado

**7.** `TabLayoutMediator` liga cada posição do ViewPager à aba correspondente no TabLayout

**8.** O ViewPager exibe a primeira aba — `NewsFragment` com category = "world"

**9.** `NewsFragment.onViewCreated()` configura o RecyclerView com `NewsAdapter` e chama `loadNews()`

**10.** `loadNews()` verifica `ConnectivityManager` — se há internet, chama `NewsApiService.getTopHeadlines()`

**11.** Retrofit monta a URL `https://newsapi.org/v2/top-headlines?language=en&pageSize=30&apiKey=...` e envia via OkHttp

**12.** A resposta JSON é convertida automaticamente pelo GsonConverter em `NewsResponse` → `List<Article>`

**13.** `adapter.setArticles(articles)` é chamado no main thread → RecyclerView renderiza os cards

**14.** Para cada card, `NewsAdapter.bind()` é chamado:
- Seta título e descrição em inglês imediatamente
- Chama `TranslationManager.translate()` em background thread
- Glide baixa a imagem da URL e aplica crossfade no `ImageView`
- Se o dispositivo estiver em PT/ES/outro → MyMemory API traduz e atualiza o texto no card

**15.** Em paralelo, `saveToDatabase()` salva os artigos no Room (SQLite) para uso offline

**16.** Se o usuário navegar para a aba **Ao Vivo**:
- `RealTimeFragment` busca na NewsAPI com query de conflitos/geopolítica
- Animação `pulse.xml` começa no ponto vermelho do header
- Itens com menos de 1h recebem indicador verde pulsante
- `Handler.postDelayed()` agenda nova busca em 5 minutos

**17.** Se o usuário navegar para a aba **Meu Estado**:
- `LocalNewsFragment` verifica permissão `ACCESS_COARSE_LOCATION`
- Se não tiver permissão → lança dialog do sistema
- Com permissão → `FusedLocationProviderClient.getCurrentLocation()` retorna lat/lon
- `Geocoder.getFromLocation()` em background thread converte para nome do estado
- Se estado tem RSS mapeado → `RssParser` baixa XML via `HttpURLConnection` e parseia com `XmlPullParser`
- Se estado não tem RSS → `NewsApiService.getEverything(query = "estado+noticias", language = "pt")`
- Resultado convertido para `List<Article>` e passado ao mesmo `NewsAdapter`

**18.** Se o usuário puxar a lista para baixo → `SwipeRefreshLayout` aciona novo ciclo a partir do passo 10

**19.** Se não houver internet em nenhuma aba → Room retorna a última lista salva localmente e um Toast informa "modo offline"
