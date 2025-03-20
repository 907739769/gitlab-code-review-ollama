package cn.jackding.gitlabcodereviewollama.server;

import cn.jackding.gitlabcodereviewollama.config.ApiConfig;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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
        String system = "你是审查代码更改的高级开发人员";
        if (StringUtils.hasText(apiConfig.getOllamaSystem())) {
            system = apiConfig.getOllamaSystem();
        }
        String prompt = "### 任务：\\n分析git diff代码片段以确定代码的质量。从总结代码变更、新的代码或者修改的代码逻辑错误、安全性问题、最佳实践建议四个方面分析给出结论。\\n\\n### 准则：\\n- 使用MarkDown**独家**响应。严禁任何形式的额外评论、解释或附加文本，要求回答言简意赅。\\n- 从总结代码变更、新的代码或者修改的代码逻辑错误、安全性问题、最佳实践建议四个方面分析给出结论\\n- 返回格式严格要求使用MarkDown\\n- 如果代码逻辑存在问题，则指出问题所在，并给出修改后的代码\\n- 如果存在安全问题，则指出问题所在，并给出修改后的代码\\n\\n### 输出：\\n严格以 MarkDown 格式返回：\\n- [x] 代码变更总结：\\n- [x] 代码逻辑：\\n- [x] 安全问题：\\n- [x] 建议：\\n\\n### git diff 代码片段：\\n";
        if (StringUtils.hasText(apiConfig.getOllamaPrompt())) {
            prompt = apiConfig.getOllamaPrompt();
        }
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", apiConfig.getOllamaModel());
        requestBody.put("system", system);
        requestBody.put("prompt", prompt + "\n" + code);
        requestBody.put("stream", false);
        //补充参数
        JSONObject options = new JSONObject();
        if(StringUtils.hasText(apiConfig.getApiOllamaOptions())){
            options=JSONObject.parseObject(apiConfig.getApiOllamaOptions());
        }else {
            options.put("temperature",0.6);

        }
        requestBody.put("options", options);

        log.info("ollama checked requestBody：" + requestBody.toJSONString());

        RequestBody body = RequestBody.create(mediaType, requestBody.toJSONString());
        Request request = new Request.Builder()
                .url(apiConfig.getOllamaUrl() + "/api/generate")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            String responseBody = response.body().string();
            log.info("ollama responseBody: " + responseBody);
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
                String comment;
                if (StringUtils.hasText(diffContent)) {
                    // 对每个文件的 diff 内容进行检查
                    JSONObject checkResult = checkCodeWithOllama(diffContent);
                    String result = checkResult.getString("response");
                    comment = "## Check result for file " + filePath + "\n" + result;
                } else {
                    comment = "## Check result for file " + filePath + "\n" + "no change";
                }
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