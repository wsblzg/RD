package com.example.interceptor;

import com.example.common.Result;
import com.example.common.JwtUtil;
import com.example.entity.YcUserAccount;
import com.example.mapper.YcCollectibleMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.List;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(JwtInterceptor.class);

    // 无需认证的精确路径
    private static final List<String> PUBLIC_EXACT_PATHS = Arrays.asList(
        "/shop/products",
        "/shop/payment-config"
    );

    // 无需认证的前缀路径
    private static final List<String> PUBLIC_PREFIX_PATHS = Arrays.asList(
        "/collectibles/",
        "/admin/collectibles/",
        "/api/collectibles/",
        "/api/admin/collectibles/",
        "/project-media/",
        "/shop/products/",
        "/api/ai/"
    );

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private YcCollectibleMapper ycCollectibleMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String uri = request.getRequestURI();
        log.debug("JwtInterceptor处理请求: {}", uri);

        if (isAiPublicPath(uri)) {
            log.info("Public AI endpoint bypassed JWT auth: {}", uri);
            return true;
        }

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return true;
        }

        if (isPublicPath(uri)) {
            log.debug("放行无需认证的接口: {}", uri);
            return true;
        }

        String tokenHeader = request.getHeader("Authorization");
        if (tokenHeader == null || !tokenHeader.startsWith("Bearer ")) {
            log.warn("Authorization头缺失或格式不正确, uri={}", uri);
            writeJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, 401, "请先登录");
            return false;
        }

        String token = tokenHeader.substring(7);
        if (!jwtUtil.validateToken(token)) {
            log.warn("Token验证失败, uri={}", uri);
            writeJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, 401, "登录已失效，请重新登录");
            return false;
        }

        try {
            String username = jwtUtil.getUsernameFromToken(token);
            return handleYcUserAuth(request, response, uri, username);
        } catch (Exception e) {
            log.error("JwtInterceptor异常: {}", e.getMessage(), e);
            writeJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, 401, "登录校验失败，请重新登录");
            return false;
        }
    }

    private boolean isPublicPath(String uri) {
        String normalizedUri = normalizeUri(uri);
        if (PUBLIC_EXACT_PATHS.contains(normalizedUri)) {
            return true;
        }
        for (String prefix : PUBLIC_PREFIX_PATHS) {
            if (normalizedUri.startsWith(prefix) || uri.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private boolean isAiPublicPath(String uri) {
        if (uri == null) {
            return false;
        }
        return uri.startsWith("/api/ai/") || uri.startsWith("/ai/");
    }

    private String normalizeUri(String uri) {
        if (uri == null) {
            return "";
        }
        if (uri.startsWith("/api/")) {
            return uri.substring(4);
        }
        if ("/api".equals(uri)) {
            return "/";
        }
        return uri;
    }

    private boolean handleYcUserAuth(HttpServletRequest request, HttpServletResponse response, String uri, String username) {
        YcUserAccount user = ycCollectibleMapper.selectUserByUsername(username);
        if (user == null) {
            log.warn("找不到窑创用户: {}", username);
            writeJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, 401, "账号不存在或已失效");
            return false;
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            log.warn("窑创用户状态不可用: username={}, status={}", user.getUsername(), user.getStatus());
            writeJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, 401, "账号已被禁用");
            return false;
        }

        boolean isAdmin = "admin".equalsIgnoreCase(user.getRole());
        if (uri.startsWith("/admin/") && !isAdmin) {
            log.warn("非管理员用户尝试访问窑创管理员API: username={}, role={}", user.getUsername(), user.getRole());
            writeJsonError(response, HttpServletResponse.SC_FORBIDDEN, 403, "无权限访问该接口");
            return false;
        }

        request.setAttribute("isAdmin", isAdmin);
        request.setAttribute("currentCollectibleUser", user);
        return true;
    }

    private void writeJsonError(HttpServletResponse response, int httpStatus, int code, String message) {
        if (response.isCommitted()) {
            return;
        }
        response.setStatus(httpStatus);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        try {
            response.getWriter().write(objectMapper.writeValueAsString(Result.error(code, message)));
            response.getWriter().flush();
        } catch (Exception ex) {
            log.error("写入鉴权错误响应失败: {}", ex.getMessage(), ex);
        }
    }
}
