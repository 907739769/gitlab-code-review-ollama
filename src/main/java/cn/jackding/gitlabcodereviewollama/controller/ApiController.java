package cn.jackding.gitlabcodereviewollama.controller;

import cn.jackding.gitlabcodereviewollama.server.ApiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api")
@Slf4j
public class ApiController {

    @Autowired
    private ApiService apiService;

    @RequestMapping("/check-pr/{projectId}/{prId}")
    public String checkPr(@PathVariable String projectId, @PathVariable String prId) {
        try {
            apiService.checkPr(projectId, prId);
            return "PR checked and commented successfully";
        } catch (IOException e) {
            log.error("Failed to check PR", e);
            return "Failed to check PR: " + e.getMessage();
        }
    }
}