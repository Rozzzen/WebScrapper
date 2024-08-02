package com.zhuk.test_assignment;

import com.zhuk.test_assignment.javafx.JavafxService;
import com.zhuk.test_assignment.service.ArticleService;
import com.zhuk.test_assignment.service.WebScraper;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
@Slf4j
public class TestAssignmentApplication extends Application {

    @Autowired
    private WebScraper webScraper;
    @Autowired
    private ArticleService articleService;
    @Autowired
    private JavafxService javafxService;

    @Override
    public void init() throws Exception {
        ConfigurableApplicationContext context = SpringApplication.run(TestAssignmentApplication.class);
        context.getAutowireCapableBeanFactory().autowireBean(this);
        webScraper.parse();
    }

    @Override
    public void start(Stage stage) {
        stage.setMaximized(true);
        stage.setTitle("JavaFX Text Block Layout");
        javafxService.generateLayout(stage);
        stage.show();
        stage.setOnCloseRequest(t -> {
            log.info("App closed");
            Platform.exit();
            System.exit(0);
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
