package com.zhuk.test_assignment.service;

import com.zhuk.test_assignment.entity.Article;
import com.zhuk.test_assignment.repo.ArticleRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebScraper {

    @Value("${website.url}")
    private String source;

    private final ArticleRepo articleRepo;
    public final List<String> months = List.of("СІЧНЯ", "ЛЮТОГО", "БЕРЕЗНЯ", "КВІТНЯ", "ТРАВНЯ", "ЧЕРВНЯ", "ЛИПНЯ", "СЕРПНЯ", "ВЕРЕСНЯ", "ЖОВТНЯ", "ЛИСТОПАДА", "ГРУДНЯ");

    @Scheduled(cron = "0 */20 * * * *")
    @Transactional
    public void parse() throws IOException {
        Document doc = Jsoup.connect(source)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/127.0.0.0 Safari/537.36")
                .header("Accept-Charset", "UTF-8")
                .referrer("http://www.google.com")
                .get();

        log.info("Scheduled call at: " + LocalDateTime.now());
        String date = mapMonth(doc.selectFirst("div.news__date").text());

        Elements elements = doc.getElementsByClass("article_news");
        for (Element element : elements) {
            //Extract headline which links to full news page
            String link = element.select("a").attr("href");
            //Ignore youtube, insta links etc
            if (link.contains("/news/")) {
                //cut /news/ off
                String linkContent = source + link.substring(6);
                var contentDoc = Jsoup.connect(linkContent).get();

                //get all article text
                String content = contentDoc.getElementsByClass("post__text").select("p").text();
                String title = generateTitle(element);
                LocalDateTime publicationTime = generatePublicationDate(date, element, contentDoc);

                if (!articleRepo.existsByTitle(title)
                        && publicationTime.isAfter(LocalDateTime.now().minusDays(1))) {

                    Article article = Article.builder()
                            .publicationDate(publicationTime)
                            .title(title)
                            .content(content)
                            .build();
                    articleRepo.save(article);
                }
            }
        }
    }

    private LocalDateTime generatePublicationDate(String date, Element element, Document contentDoc) {
        String time = element.getElementsByClass("article__time").text();
        if (time.isBlank()) {
            //Новини — Четвер, 1 серпня 2024, 18:23 into 18:23
            //Some articles don't have time in title
            String regex = "\\b(\\d{2}:\\d{2})\\b";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(contentDoc.getElementsByClass("post__time").text());
            if (matcher.find()) time = matcher.group(1);
        }
        return LocalDateTime.of(LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE), LocalTime.parse(time));
    }

    private String generateTitle(Element element) {
        String result = element.select("a").html();
        //Sometimes title contains tags with <em> tag
        if (result.startsWith("<em>"))
            result = result.replaceAll("<em>.*?</em>", "");

        return result;
    }

    private String mapMonth(String articleDate) {
        articleDate = articleDate.toUpperCase();
        for (int i = 0; i < months.size(); i++) {
            if(articleDate.contains(months.get(i))) {
                //Replace month text with numeric value
                String result = articleDate.replace(months.get(i), String.valueOf(i + 1));

                //4 8 2024 into 04 08 2024
                String [] split = result.split(" ");
                if(split[0].length() == 1) split[0] = "0" + split[0];
                if(split[1].length() == 1) split[1] = "0" + split[1];

                //04 08 2024 into 2024-08-04
                return split[2] + "-" + split[1] + "-" + split[0];
            }
        }
        // should never occur
        throw new IllegalArgumentException("Invalid month in date: " + articleDate);
    }
}
