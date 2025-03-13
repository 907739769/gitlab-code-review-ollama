package cn.jackding.gitlabcodereviewollama;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class GitlabCodeReviewOllamaApplication {

    public static void main(String[] args) {
        SpringApplication.run(GitlabCodeReviewOllamaApplication.class, args);
    }

}
