package com.example.controller.collectible;

import com.example.common.Result;
import com.example.dto.YcCaptchaVerifyDTO;
import com.example.dto.YcLoginDTO;
import com.example.dto.YcRegisterDTO;
import com.example.service.LoginCaptchaService;
import com.example.service.YcCollectibleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/collectibles/auth")
public class CollectibleAuthController {

    @Resource
    private YcCollectibleService ycCollectibleService;

    @Resource
    private LoginCaptchaService loginCaptchaService;

    @PostMapping("/register")
    public Result<Void> register(@RequestBody YcRegisterDTO dto) {
        ycCollectibleService.register(dto);
        return Result.success();
    }

    @PostMapping("/captcha/challenge")
    public Result<Map<String, Object>> captchaChallenge(HttpServletRequest request) {
        return Result.success(loginCaptchaService.createChallenge(clientIp(request)));
    }

    @PostMapping("/captcha/verify")
    public Result<Map<String, Object>> captchaVerify(@RequestBody YcCaptchaVerifyDTO dto,
                                                     HttpServletRequest request) {
        return Result.success(loginCaptchaService.verify(dto, clientIp(request)));
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody YcLoginDTO dto, HttpServletRequest request) {
        return Result.success(ycCollectibleService.login(dto, clientIp(request)));
    }

    @GetMapping("/me")
    public Result<Map<String, Object>> me(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.success(ycCollectibleService.currentUser(authorization));
    }

    private String clientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.trim().isEmpty()) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.trim().isEmpty()) {
            return realIp.trim();
        }
        return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }
}
