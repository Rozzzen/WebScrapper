package com.zhuk.test_assignment.repo;

import com.zhuk.test_assignment.entity.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface ArticleRepo extends JpaRepository<Article, Long> {
    boolean existsByTitle(String title);

    @Modifying
    @Query("""
             DELETE FROM Article article
             WHERE article.publicationDate < :oneDayAgo
            """)
    int clearArticles(LocalDateTime oneDayAgo);
}
