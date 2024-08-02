package com.zhuk.test_assignment.javafx;

import com.zhuk.test_assignment.entity.Article;
import com.zhuk.test_assignment.service.ArticleService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class JavafxService {

    @Autowired
    private ArticleService articleService;

    private int currentPage = 0;
    private Article currentArticle;

    private final Button prevButton = new Button("<");
    private final Button nextButton = new Button(">");
    private HBox textBlocksContainer;
    private VBox layout;
    private Scene mainScene, formScene;
    private Stage stage;
    private TextField titleField;
    private TextArea contentArea;

    public void generateLayout(Stage stage) {

        this.stage = stage;

        //Create main page layout
        HBox navbar = new HBox(10);
        Button createButton = new Button("Create new article");
        createButton.setOnAction(e -> showFormScene(null));
        navbar.getChildren().add(createButton);
        navbar.setPadding(new Insets(10));
        navbar.setStyle("-fx-background-color: #336699;");

        layout = new VBox(10);

        prevButton.setOnAction(e -> navigatePage(-1));
        nextButton.setOnAction(e -> navigatePage(1));

        HBox navigationBox = new HBox(20, prevButton, nextButton);
        navigationBox.setAlignment(Pos.BOTTOM_CENTER);

        textBlocksContainer = new HBox(20);
        textBlocksContainer.setAlignment(Pos.CENTER);
        textBlocksContainer.setMaxWidth(Double.MAX_VALUE);
        textBlocksContainer.setMaxHeight(Double.MAX_VALUE);

        layout.setMaxWidth(Double.MAX_VALUE);
        layout.setMaxHeight(Double.MAX_VALUE);
        layout.getChildren().addAll(navbar, textBlocksContainer, navigationBox);

        updateArticleList();

        //refresh page every 5 minutes
        Thread thread = new Thread(() -> {
            while (true) {
                try {
                    Platform.runLater(this::updateArticleList);
                    Thread.sleep(5 * 60 * 1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        thread.setDaemon(true);
        thread.start();

        mainScene = new Scene(layout, 800, 600);

        //Create form page layout
        VBox formLayout = new VBox(10);
        formLayout.setPadding(new Insets(10));

        titleField = new TextField();
        contentArea = new TextArea();

        contentArea.setPrefHeight(100);
        contentArea.setWrapText(true);

        Button submitButton = new Button("Submit");

        submitButton.setOnAction(e -> {
            articleService.save(Article.builder()
                    .id(currentArticle != null ? currentArticle.getId() : null)
                    .title(titleField.getText())
                    .content(contentArea.getText())
                    .publicationDate(LocalDateTime.now()).build());

            titleField.clear();
            contentArea.clear();
            updateArticleList();
            stage.setScene(mainScene);
            currentArticle = null;
        });
        Button cancelButton = new Button("Cancel");
        cancelButton.setOnAction(e -> stage.setScene(mainScene));

        formLayout.getChildren().addAll(new Label("Title:"), titleField, new Label("Content:"), contentArea, submitButton, cancelButton);

        formScene = new Scene(formLayout, 800, 600);

        stage.setScene(mainScene);
    }

    private void showFormScene(Article article) {
        if (article != null) {
            titleField.setText(article.getTitle());
            contentArea.setText(article.getContent());
            currentArticle = article;
        } else {
            titleField.clear();
            contentArea.clear();
        }
        this.stage.setScene(formScene);
    }

    private VBox createTextBlock(Article article) {
        VBox vbox = new VBox(5);
        Text headerText = new Text(article.getTitle() + "\n\n");
        headerText.setFont(Font.font("Arial", FontWeight.BOLD, 24));

        Text contentText = new Text(article.getContent());
        contentText.setFont(Font.font("Arial", FontWeight.NORMAL, 16));

        TextFlow textFlow = new TextFlow(headerText, contentText);
        textFlow.setMinHeight(800);
        textFlow.setStyle("-fx-background-color: lightgray; -fx-padding: 10px;");

        Button deleteButton = new Button("Delete");
        Button editButton = new Button("Edit");

        deleteButton.setOnAction(e -> {
            articleService.delete(article);
            updateArticleList();
        });

        editButton.setOnAction(e -> {
            showFormScene(article);
        });

        ScrollPane scrollPane = new ScrollPane(textFlow);
        scrollPane.setFitToWidth(true);
        scrollPane.setMinHeight(800);
        scrollPane.setStyle("-fx-background: lightgray; -fx-border-color: gray;");

        vbox.getChildren().addAll(scrollPane, deleteButton, editButton);
        return vbox;
    }

    private void navigatePage(int direction) {
        currentPage += direction;
        updateButtonStates();
        updateArticleList();
    }

    private void updateButtonStates() {
        prevButton.setDisable(currentPage == 0);
        nextButton.setDisable(currentPage == articleService.countPages() - 1);
    }

    private void updateArticleList() {
        Page<Article> articlePage = articleService.findAllArticles(currentPage);
        List<Article> articles = articlePage.getContent();

        VBox[] textBlocks = new VBox[articlePage.getNumberOfElements()];

        for (int i = 0; i < articlePage.getNumberOfElements(); i++)
            textBlocks[i] = createTextBlock(articles.get(i));

        for (VBox scrollPane : textBlocks) {
            scrollPane.prefWidthProperty().bind(layout.widthProperty().divide(3));
        }

        textBlocksContainer.getChildren().clear();
        textBlocksContainer.getChildren().addAll(textBlocks);
    }
}
