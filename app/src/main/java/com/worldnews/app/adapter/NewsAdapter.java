package com.worldnews.app.adapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.worldnews.app.R;
import com.worldnews.app.model.Article;
import com.worldnews.app.utils.TranslationManager;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.NewsViewHolder> {

    private final Context context;
    private List<Article> articles;

    public NewsAdapter(Context context) {
        this.context = context;
        this.articles = new ArrayList<>();
    }

    public void setArticles(List<Article> articles) {
        this.articles = articles != null ? articles : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void addArticles(List<Article> newArticles) {
        if (newArticles == null) return;
        int start = this.articles.size();
        this.articles.addAll(newArticles);
        notifyItemRangeInserted(start, newArticles.size());
    }

    @NonNull
    @Override
    public NewsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_news, parent, false);
        return new NewsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NewsViewHolder holder, int position) {
        Article article = articles.get(position);
        holder.bind(article);
    }

    @Override
    public int getItemCount() {
        return articles.size();
    }

    class NewsViewHolder extends RecyclerView.ViewHolder {

        private final ImageView ivCover;
        private final TextView tvTitle;
        private final TextView tvDescription;
        private final TextView tvSource;
        private final TextView tvTime;
        private final ImageButton btnShare;

        public NewsViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCover = itemView.findViewById(R.id.iv_cover);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvDescription = itemView.findViewById(R.id.tv_description);
            tvSource = itemView.findViewById(R.id.tv_source);
            tvTime = itemView.findViewById(R.id.tv_time);
            btnShare = itemView.findViewById(R.id.btn_share);
        }

        public void bind(Article article) {
            // Guarda a URL como tag para validar callbacks de tradução após reciclagem
            itemView.setTag(article.getUrl());

            // Título — exibe original, depois atualiza com tradução se necessário
            String originalTitle = article.getTitle() != null ? article.getTitle() : "";
            tvTitle.setText(originalTitle);
            TranslationManager.getInstance().translate(originalTitle, translated -> {
                if (article.getUrl() != null && article.getUrl().equals(itemView.getTag())) {
                    tvTitle.setText(translated);
                }
            });

            // Descrição
            if (!TextUtils.isEmpty(article.getDescription())) {
                tvDescription.setVisibility(View.VISIBLE);
                String originalDesc = article.getDescription();
                tvDescription.setText(originalDesc);
                TranslationManager.getInstance().translate(originalDesc, translated -> {
                    if (article.getUrl() != null && article.getUrl().equals(itemView.getTag())) {
                        tvDescription.setText(translated);
                    }
                });
            } else {
                tvDescription.setVisibility(View.GONE);
            }

            // Fonte
            if (article.getSource() != null && !TextUtils.isEmpty(article.getSource().getName())) {
                tvSource.setText(article.getSource().getName());
            } else {
                tvSource.setText("");
            }

            // Tempo relativo
            tvTime.setText(getRelativeTime(article.getPublishedAt()));

            // Imagem
            if (!TextUtils.isEmpty(article.getUrlToImage())) {
                ivCover.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(article.getUrlToImage())
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .placeholder(R.drawable.placeholder_news)
                        .error(R.drawable.placeholder_news)
                        .centerCrop()
                        .into(ivCover);
            } else {
                ivCover.setVisibility(View.GONE);
            }

            // Clique no card abre no browser
            itemView.setOnClickListener(v -> {
                if (!TextUtils.isEmpty(article.getUrl())) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(article.getUrl()));
                    context.startActivity(intent);
                }
            });

            // Botão compartilhar
            btnShare.setOnClickListener(v -> {
                String title = article.getTitle() != null ? article.getTitle() : "";
                String url = article.getUrl() != null ? article.getUrl() : "";
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, title);
                shareIntent.putExtra(Intent.EXTRA_TEXT, title + "\n\n" + url);
                context.startActivity(Intent.createChooser(shareIntent, "Compartilhar notícia"));
            });
        }

        private String getRelativeTime(String publishedAt) {
            if (TextUtils.isEmpty(publishedAt)) return "";
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date date = sdf.parse(publishedAt);
                if (date == null) return "";

                long diffMs = System.currentTimeMillis() - date.getTime();
                long minutes = TimeUnit.MILLISECONDS.toMinutes(diffMs);
                long hours = TimeUnit.MILLISECONDS.toHours(diffMs);
                long days = TimeUnit.MILLISECONDS.toDays(diffMs);

                if (minutes < 1) return "agora mesmo";
                if (minutes < 60) return "há " + minutes + " min";
                if (hours < 24) return "há " + hours + "h";
                if (days == 1) return "ontem";
                return "há " + days + " dias";
            } catch (ParseException e) {
                return "";
            }
        }
    }
}
