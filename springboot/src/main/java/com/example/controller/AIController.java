package com.example.controller;

import com.example.common.Result;
import com.example.service.AIChatQueueService;
import com.example.service.AIService;
import com.example.service.XilingTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

/**
 * AI功能控制器
 */
@RestController
@RequestMapping("/api/ai")
public class AIController {

    @Autowired
    private AIService aiService;

    @Autowired
    private AIChatQueueService aiChatQueueService;

    @Autowired
    private XilingTokenService xilingTokenService;

    /**
     * 生成图片描述
     */
    @PostMapping("/generate-image-description")
    public Result<String> generateImageDescription(@RequestParam("file") MultipartFile file) {
        try {
            String description = aiService.generateImageDescription(file);
            return Result.success(description);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("生成图片描述失败: " + e.getMessage());
        }
    }

    /**
     * 根据base64生成图片描述
     */
    @PostMapping("/generate-image-description-base64")
    public Result<String> generateImageDescriptionFromBase64(@RequestBody String imageBase64) {
        try {
            String description = aiService.generateImageDescriptionFromBase64(imageBase64);
            return Result.success(description);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("生成图片描述失败: " + e.getMessage());
        }
    }

    /**
     * 生成文章摘要
     */
    @PostMapping("/generate-summary")
    public Result<String> generateSummary(@RequestParam String content,
                                           @RequestParam(defaultValue = "150") int maxLength) {
        try {
            String summary = aiService.generateArticleSummary(content, maxLength);
            return Result.success(summary);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("生成摘要失败: " + e.getMessage());
        }
    }

    /**
     * 推荐文章
     */
    @GetMapping("/recommend-articles")
    public Result<String> recommendArticles(@RequestParam Long userId,
                                             @RequestParam(defaultValue = "5") int limit) {
        try {
            String recommendations = aiService.recommendArticles(userId, limit);
            return Result.success(recommendations);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取推荐失败: " + e.getMessage());
        }
    }

    /**
     * 智能客服问答
     */
    @PostMapping("/chatbot")
    public Result<String> chatbot(@RequestParam String question,
                                   @RequestParam(required = false) String context) {
        try {
            String answer = aiChatQueueService.submitAndWait(question, context);
            return Result.success(answer);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("AI客服暂时不可用: " + e.getMessage());
        }
    }


    @GetMapping("/chatbot/queue-metrics")
    public Result<Map<String, Object>> chatbotQueueMetrics() {
        return Result.success(aiChatQueueService.metrics());
    }

    @GetMapping("/xiling/token")
    public Result<String> xilingToken(
            @RequestParam(defaultValue = "24") int expireHours
    ) {
        try {
            return Result.success(xilingTokenService.generateToken(expireHours));
        } catch (Exception e) {
            return Result.error("曦灵动态令牌生成失败: " + e.getMessage());
        }
    }


    @PostMapping("/moderate")
    public Result<String> moderate(@RequestParam String content) {
        try {
            String result = aiService.moderateContent(content);
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("审核失败: " + e.getMessage());
        }
    }
}
