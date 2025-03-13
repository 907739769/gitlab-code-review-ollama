package cn.jackding.gitlabcodereviewollama.controller;

import cn.jackding.gitlabcodereviewollama.server.ApiService;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/webhook")
@Slf4j
public class WebhookController {

    @Autowired
    private ApiService apiService;

    @PostMapping("/gitlab")
    public ResponseEntity<String> handleGitLabWebhook(@RequestBody JSONObject webhookPayload) {
        log.info("webhookPayload: " + webhookPayload.toJSONString());
        try {
            // 检查事件类型是否为 Merge Request Event
            String eventType = webhookPayload.getString("object_kind");
            if ("merge_request".equals(eventType)) {
                // 获取 PR 状态
                String action = webhookPayload.getJSONObject("object_attributes").getString("action");

                // 只对打开状态的 PR 进行处理
                if ("open".equals(action) || "reopen".equals(action)) {
                    String projectId = String.valueOf(webhookPayload.getJSONObject("project").getLong("id"));
                    String prId = String.valueOf(webhookPayload.getJSONObject("object_attributes").getLong("iid"));

                    // 异步处理代码检查
                    handlePrCheckAsync(projectId, prId);
                } else {
                    log.info("Received PR action: {}, skipping processing", action);
                }
            } else {
                log.info("Received unsupported event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Failed to handle GitLab webhook", e);
            return new ResponseEntity<>("Error handling webhook", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>("Webhook processed successfully", HttpStatus.OK);
    }

    @Async
    public void handlePrCheckAsync(String projectId, String prId) {
        try {
            apiService.checkPr(projectId, prId);
        } catch (Exception e) {
            log.error("Failed to check PR asynchronously", e);
        }
    }
}