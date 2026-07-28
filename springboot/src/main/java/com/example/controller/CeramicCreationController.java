package com.example.controller;

import com.example.common.Result;
import com.example.entity.YcUserAccount;
import com.example.exception.CustomException;
import com.example.mapper.YcCollectibleMapper;
import com.example.service.AIService;
import com.example.service.TencentAi3dService;
import com.example.common.JwtUtil;
import org.springframework.core.io.InputStreamResource;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ceramic-creation")
public class CeramicCreationController {
    private final TencentAi3dService tencentAi3dService;
    private final AIService aiService;
    private final YcCollectibleMapper ycCollectibleMapper;
    private final JwtUtil jwtUtil;

    public CeramicCreationController(TencentAi3dService tencentAi3dService,
                                     AIService aiService,
                                     YcCollectibleMapper ycCollectibleMapper,
                                     JwtUtil jwtUtil) {
        this.tencentAi3dService = tencentAi3dService;
        this.aiService = aiService;
        this.ycCollectibleMapper = ycCollectibleMapper;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/prompt/optimize")
    public Result<String> optimizePrompt(@RequestBody Map<String, String> body,
                                         @RequestHeader(value = "Authorization", required = false) String authorization) {
        try {
            requireUser(authorization);
            String message = body == null ? null : body.get("message");
            if (!StringUtils.hasText(message)) {
                return Result.error("请先填写创作描述");
            }
            return Result.success(aiService.optimizeCeramicPrompt(message, body.get("style"), body.get("vessel")));
        } catch (Exception e) {
            return safeError("提示词优化失败，请稍后重试", e);
        }
    }

    @PostMapping("/model")
    public Result<Map<String, Object>> createModel(@RequestParam(required = false) String prompt,
                                                   @RequestParam(required = false) String style,
                                                   @RequestParam(required = false) String vessel,
                                                   @RequestHeader(value = "Authorization", required = false) String authorization,
                                                   @RequestParam(required = false, name = "image") MultipartFile image) {
        try {
            YcUserAccount userAccount = requireUser(authorization);
            return Result.success(tencentAi3dService.createModelTask(userAccount, prompt, style, vessel, image));
        } catch (Exception e) {
            return safeError("作品生成请求提交失败，请稍后重试", e);
        }
    }

    @GetMapping("/model/{taskId}")
    public Result<Map<String, Object>> queryModel(@PathVariable String taskId,
                                                  @RequestHeader(value = "Authorization", required = false) String authorization) {
        try {
            YcUserAccount userAccount = requireUser(authorization);
            return Result.success(tencentAi3dService.queryModelTask(userAccount.getId(), taskId));
        } catch (Exception e) {
            return safeError("作品生成进度查询失败，请稍后重试", e);
        }
    }

    @GetMapping("/works")
    public Result<List<Map<String, Object>>> myWorks(@RequestParam(required = false) Integer limit,
                                                     @RequestParam(required = false, defaultValue = "all") String scope,
                                                     @RequestHeader(value = "Authorization", required = false) String authorization) {
        try {
            YcUserAccount userAccount = requireUser(authorization);
            return Result.success(tencentAi3dService.listUserModelWorks(userAccount.getId(), scope, limit));
        } catch (Exception e) {
            return safeError("作品列表加载失败，请稍后重试", e);
        }
    }

    @GetMapping("/session")
    public Result<Map<String, Object>> modelSession(@RequestHeader(value = "Authorization", required = false) String authorization) {
        try {
            YcUserAccount userAccount = requireUser(authorization);
            return Result.success(tencentAi3dService.getModelSession(userAccount.getId()));
        } catch (Exception e) {
            return safeError("创作进度加载失败，请稍后重试", e);
        }
    }

    @GetMapping("/works/latest")
    public Result<Map<String, Object>> latestWork(@RequestHeader(value = "Authorization", required = false) String authorization) {
        try {
            YcUserAccount userAccount = requireUser(authorization);
            Map<String, Object> session = tencentAi3dService.getModelSession(userAccount.getId());
            return Result.success((Map<String, Object>) session.get("latestPreview"));
        } catch (Exception e) {
            return safeError("最近作品加载失败，请稍后重试", e);
        }
    }

    @GetMapping("/works/{id}")
    public Result<Map<String, Object>> workDetail(@PathVariable Long id,
                                                  @RequestHeader(value = "Authorization", required = false) String authorization) {
        try {
            YcUserAccount requester = optionalUser(authorization);
            return Result.success(tencentAi3dService.getModelWorkById(id, requester == null ? null : requester.getId()));
        } catch (Exception e) {
            return safeError("作品详情加载失败，请稍后重试", e);
        }
    }

    @PostMapping("/model/{taskId}/save")
    public Result<Map<String, Object>> saveModel(@PathVariable String taskId,
                                                 @RequestParam(required = false) String title,
                                                 @RequestParam(required = false) String prompt,
                                                 @RequestParam(required = false) String style,
                                                 @RequestParam(required = false) String vessel,
                                                 @RequestHeader(value = "Authorization", required = false) String authorization) {
        try {
            YcUserAccount userAccount = requireUser(authorization);
            return Result.success(tencentAi3dService.persistModelWork(userAccount, taskId));
        } catch (Exception e) {
            return safeError("作品永久保存失败，请稍后重试", e);
        }
    }

    @GetMapping("/model-file")
    public ResponseEntity<InputStreamResource> modelFile(
            @RequestParam String url,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) String range) {
        try {
            return tencentAi3dService.fetchModelFile(url, range);
        } catch (Exception e) {
            byte[] message = "模型文件暂时无法读取".getBytes(StandardCharsets.UTF_8);
            return ResponseEntity.badRequest()
                    .contentType(MediaType.TEXT_PLAIN)
                    .contentLength(message.length)
                    .body(new InputStreamResource(new ByteArrayInputStream(message)));
        }
    }

    private YcUserAccount requireUser(String authorization) {
        YcUserAccount user = optionalUser(authorization);
        if (user == null) {
            throw new CustomException("401", "请先登录");
        }
        return user;
    }

    private YcUserAccount optionalUser(String authorization) {
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            return null;
        }
        String token = authorization.substring(7).trim();
        if (!jwtUtil.validateToken(token)) {
            throw new CustomException("401", "登录状态已失效");
        }
        String username = jwtUtil.getUsernameFromToken(token);
        YcUserAccount user = ycCollectibleMapper.selectUserByUsername(username);
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new CustomException("403", "账号暂不可用");
        }
        return user;
    }

    private <T> Result<T> safeError(String fallback, Exception error) {
        if (error instanceof CustomException) {
            return Result.error(error.getMessage());
        }
        return Result.error(fallback);
    }
}
