package com.worldnews.app.adapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
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

public class RealTimeAdapter extends RecyclerView.Adapter<RealTimeAdapter.RealTimeViewHolder> {

    private final Context context;
    private List<Article> articles;

    public RealTimeAdapter(Context context) {
        this.context = context;
        this.articles = new ArrayList<>();
    }

    public void setArticles(List<Article> articles) {
        this.articles = articles != null ? articles : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RealTimeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_realtime, parent, false);
        return new RealTimeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RealTimeViewHolder holder, int position) {
        Article article = articles.get(position);
        holder.bind(article);
    }

    @Override
    public int getItemCount() {
        return articles.size();
    }

    class RealTimeViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvTitle;
        private final TextView tvSource;
        private final TextView tvTime;
        private final View viewIndicator;

        public RealTimeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_rt_title);
            tvSource = itemView.findViewById(R.id.tv_rt_source);
            tvTime = itemView.findViewById(R.id.tv_rt_time);
            viewIndicator = itemView.findViewById(R.id.view_indicator);
        }

        public void bind(Article article) {
            itemView.setTag(article.getUrl());

            String originalTitle = article.getTitle() != null ? article.getTitle() : "";
            tvTitle.setText(originalTitle);
            TranslationManager.getInstance().translate(originalTitle, translated -> {
                if (article.getUrl() != null && article.getUrl().equals(itemView.getTag())) {
                    tvTitle.setText(translated);
                }
            });

            if (article.getSource() != null && !TextUtils.isEmpty(article.getSource().getName())) {
                tvSource.setText(article.getSource().getName());
            } else {
                tvSource.setText("");
            }

            String relTime = getRelativeTime(article.getPublishedAt());
            tvTime.setText(relTime);

            // Indicador de urgência: verde pulsante se recente (< 1h), amarelo se < 6h, cinza se mais antigo
            long diffHours = getDiffHours(article.getPublishedAt());
            viewIndicator.clearAnimation();
            if (diffHours < 1) {
                viewIndicator.setBackgroundColor(0xFF00C853); // verde
                Animation pulse = AnimationUtils.loadAnimation(context, R.anim.pulse);
                viewIndicator.startAnimation(pulse);
            } else if (diffHours < 6) {
                viewIndicator.setBackgroundColor(0xFFFFD600); // amarelo
            } else {
                viewIndicator.setBackgroundColor(0xFF757575); // cinza
            }

            itemView.setOnClickListener(v -> {
                if (!TextUtils.isEmpty(article.getUrl())) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(article.getUrl()));
                    context.startActivity(intent);
                }
            });
        }

        private long getDiffHours(String publishedAt) {
            if (TextUtils.isEmpty(publishedAt)) return 99;
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date date = sdf.parse(publishedAt);
                if (date == null) return 99;
                long diffMs = System.currentTimeMillis() - date.getTime();
                return TimeUnit.MILLISECONDS.toHours(diffMs);
            } catch (ParseException e) {
                return 99;
            }
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
                if (minutes < 60) return "há " + minutes + " minutos";
                if (hours < 24) return "há " + hours + " horas";
                if (days == 1) return "há 1 dia";
                return "há " + days + " dias";
            } catch (ParseException e) {
                return "";
            }
        }
    }
}
