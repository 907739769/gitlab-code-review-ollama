package cn.jackding.gitlabcodereviewollama.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Data
@Component
public class ApiConfig {
    @Value("${apiGitlabUrl}")
    private String gitlabUrl;

    @Value("${apiGitlabToken}")
    private String gitlabToken;

    @Value("${apiOllamaUrl}")
    private String ollamaUrl;

    @Value("${apiOllamaModel}")
    private String ollamaModel;

    @Value("${apiOllamaSystem:}")
    private String ollamaSystem;

    @Value("${apiOllamaPrompt:}")
    private String ollamaPrompt;

}
