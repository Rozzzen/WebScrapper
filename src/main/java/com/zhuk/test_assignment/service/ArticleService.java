package com.zhuk.test_assignment.service;

import com.zhuk.test_assignment.entity.Article;
import com.zhuk.test_assignment.repo.ArticleRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleRepo articleRepo;
    private final int PAGE_SIZE = 3;

    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void clearNews() {
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);

        int deletedCount = articleRepo.clearArticles(oneDayAgo);
        log.info("Cleaned up " + deletedCount + " articles");
    }

    public Page<Article> findAllArticles(int page) {
        Pageable pageable = PageRequest.of(page, PAGE_SIZE, Sort.by("publicationDate").descending());
        return articleRepo.findAll(pageable);
    }

    public long countPages() {
        long count = articleRepo.count();
        long result = count / PAGE_SIZE;
        if (count % PAGE_SIZE != 0) result++;
        return result;
    }

    public void save(Article article) {
        articleRepo.save(article);
    }

    public void delete(Article article) {
        articleRepo.delete(article);
    }
}
