package cn.jackding.gitlabcodereviewollama.server;

import cn.jackding.gitlabcodereviewollama.config.ApiConfig;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ApiService {

    private final OkHttpClient client;

    @Autowired
    private ApiConfig apiConfig;

    public ApiService() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(300, TimeUnit.SECONDS)
                .writeTimeout(300, TimeUnit.SECONDS)
                .build();
    }

    public JSONObject checkCodeWithOllama(String code) throws IOException {
        log.info("ollama checked code:" + code);
        MediaType mediaType = MediaType.get("application/json; charset=utf-8");
        // 构建提示词
        String system = "您是审查代码更改的高级开发人员。" +
                "问题：\n" +
                "1. 总结关键变化。\n" +
                "2. 新的/修改后的代码是否清晰？\n" +
                "3. 注释和名称是否具有描述性？\n" +
                "4. 是否可以降低复杂性？举例说明？\n" +
                "5. 有错误吗？在哪里？\n" +
                "6. 潜在的安全问题？\n" +
                "7. 最佳实践建议？";
        String prompt = "在回复中包括每个问题的简洁版本。检查以下 git diff 代码更改，重点关注结构、安全性和清晰度：\n\n" + code;
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", apiConfig.getOllamaModel());
        requestBody.put("system", system);
        requestBody.put("prompt", prompt);
        requestBody.put("stream", false);

        RequestBody body = RequestBody.create(mediaType, requestBody.toJSONString());
        Request request = new Request.Builder()
                .url(apiConfig.getOllamaUrl() + "/api/generate")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            String responseBody = response.body().string();
            log.debug("ollama responseBody: " + responseBody);
            return JSON.parseObject(responseBody);
        }
    }

    public void addCommentToPr(String projectId, String prId, String comment) throws IOException {
        log.info("comment: " + comment);
        MediaType mediaType = MediaType.get("application/json; charset=utf-8");
        JSONObject requestBody = new JSONObject();
        requestBody.put("body", comment);
        RequestBody body = RequestBody.create(mediaType, requestBody.toJSONString());
        Request request = new Request.Builder()
                .url(apiConfig.getGitlabUrl() + "/projects/" + projectId + "/merge_requests/" + prId + "/notes")
                .header("Private-Token", apiConfig.getGitlabToken())
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            response.body().string();
        }
    }

    public JSONObject getPrChanges(String projectId, String prId) throws IOException {
        Request request = new Request.Builder()
                .url(apiConfig.getGitlabUrl() + "/projects/" + projectId + "/merge_requests/" + prId + "/changes")
                .header("Private-Token", apiConfig.getGitlabToken())
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            String responseBody = response.body().string();
            log.info("responseBody: " + responseBody);
            return JSON.parseObject(responseBody);
        }
    }

    public void checkPr(String projectId, String prId) throws IOException {
        JSONObject changes = getPrChanges(projectId, prId);
        if (changes.containsKey("changes")) {
            for (Object changeObj : changes.getJSONArray("changes")) {
                JSONObject change = (JSONObject) changeObj;
                String filePath = change.getString("new_path");
                String diffContent = change.getString("diff");

                // 对每个文件的 diff 内容进行检查
                JSONObject checkResult = checkCodeWithOllama(diffContent);
                String result = checkResult.getString("response");
                String comment = "## Check result for file " + filePath + ": \n\n" + result;

                // 添加评论到 PR
                addCommentToPr(projectId, prId, comment);
            }
        }
    }

    @Data
    public static class Commit {
        private String id;
        // other fields can be added as necessary
    }
}