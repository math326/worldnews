package com.worldnews.app.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface ArticleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertArticle(ArticleEntity article);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertArticles(List<ArticleEntity> articles);

    @Query("SELECT * FROM articles WHERE category = :category ORDER BY savedAt DESC LIMIT 50")
    List<ArticleEntity> getArticlesByCategory(String category);

    @Query("SELECT * FROM articles ORDER BY savedAt DESC LIMIT 100")
    List<ArticleEntity> getAllArticles();

    @Query("DELETE FROM articles WHERE category = :category")
    void deleteArticlesByCategory(String category);

    @Query("DELETE FROM articles WHERE savedAt < :timestamp")
    void deleteOldArticles(long timestamp);

    @Query("SELECT COUNT(*) FROM articles WHERE category = :category")
    int countByCategory(String category);
}
