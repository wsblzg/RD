package com.example.service;

import com.example.common.JwtUtil;
import com.example.dto.YcPointsRechargeCreateDTO;
import com.example.dto.YcPointsRechargeReviewDTO;
import com.example.entity.YcPointsRechargeOrder;
import com.example.entity.YcAiModelWork;
import com.example.entity.YcUserAccount;
import com.example.exception.CustomException;
import com.example.mapper.YcCollectibleMapper;
import com.example.mapper.YcPointsMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class YcPointsService {
    public static final int AI3D_COST_POINTS = 10;
    public static final int AI3D_PERSIST_COST_POINTS = 10;
    public static final int RECHARGE_POINTS_PER_YUAN = 10;

    private static final String STATUS_PENDING_PAYMENT = "PENDING_PAYMENT";
    private static final String STATUS_PENDING_REVIEW = "PENDING_REVIEW";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final DateTimeFormatter ORDER_NO_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Resource
    private YcPointsMapper ycPointsMapper;

    @Resource
    private YcCollectibleMapper ycCollectibleMapper;

    @Resource
    private JwtUtil jwtUtil;

    @Value("${shop.payment.qr-url:/picket.webp}")
    private String paymentQrUrl;

    public Map<String, Object> getSummary(String authorization) {
        YcUserAccount user = requireLogin(authorization);
        return toSummary(user);
    }

    public Map<String, Object> getSummary(YcUserAccount user) {
        return toSummary(user);
    }

    public int getAi3dCostPoints() {
        return AI3D_COST_POINTS;
    }

    public int getAi3dPersistCostPoints() {
        return AI3D_PERSIST_COST_POINTS;
    }

    @Transactional
    public Map<String, Object> createRecharge(YcPointsRechargeCreateDTO dto, String authorization) {
        YcUserAccount user = requireLogin(authorization);
        if (isUnlimitedPointsUser(user)) {
            throw new CustomException("400", "管理员账号积分额度无限，无需充值");
        }
        BigDecimal amount = normalizeAmount(dto == null ? null : dto.getAmount());
        int points = amount.multiply(BigDecimal.valueOf(RECHARGE_POINTS_PER_YUAN))
                .setScale(0, RoundingMode.DOWN)
                .intValue();
        if (points <= 0) {
            throw new CustomException("400", "充值金额过小");
        }

        YcPointsRechargeOrder order = new YcPointsRechargeOrder();
        order.setRechargeNo(generateRechargeNo(user.getId()));
        order.setUserId(user.getId());
        order.setAmount(amount);
        order.setPointsAmount(points);
        order.setStatus(STATUS_PENDING_PAYMENT);
        ycPointsMapper.insertRechargeOrder(order);
        return toRechargeView(ycPointsMapper.selectRechargeOrderById(order.getId()));
    }

    public Map<String, Object> listMyRecharges(Integer page, Integer pageSize, String authorization) {
        YcUserAccount user = requireLogin(authorization);
        int p = normalizePage(page);
        int ps = normalizePageSize(pageSize);
        List<YcPointsRechargeOrder> list = ycPointsMapper.selectUserRechargeOrders(user.getId(), (p - 1) * ps, ps);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", list.stream().map(this::toRechargeView).collect(Collectors.toList()));
        result.put("page", p);
        result.put("pageSize", ps);
        return result;
    }

    @Transactional
    public Map<String, Object> markRechargePaid(Long id, String authorization) {
        YcUserAccount user = requireLogin(authorization);
        if (id == null) {
            throw new CustomException("400", "充值单ID不能为空");
        }
        int affected = ycPointsMapper.markRechargePaid(id, user.getId());
        if (affected <= 0) {
            throw new CustomException("409", "充值单状态不可提交付款");
        }
        return toRechargeView(ycPointsMapper.selectRechargeOrderById(id));
    }

    public Map<String, Object> adminListRecharges(String keyword, String status, Integer page, Integer pageSize, String authorization) {
        requireAdmin(authorization);
        int p = normalizePage(page);
        int ps = normalizePageSize(pageSize);
        String normalizedKeyword = trimToNull(keyword);
        String normalizedStatus = trimToNull(status);
        int total = ycPointsMapper.countAdminRechargeOrders(normalizedKeyword, normalizedStatus);
        List<YcPointsRechargeOrder> list = ycPointsMapper.selectAdminRechargeOrders(normalizedKeyword, normalizedStatus, (p - 1) * ps, ps);
        Map<String, Object> pagination = new LinkedHashMap<>();
        pagination.put("page", p);
        pagination.put("pageSize", ps);
        pagination.put("total", total);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", list.stream().map(this::toRechargeView).collect(Collectors.toList()));
        result.put("pagination", pagination);
        return result;
    }

    @Transactional
    public Map<String, Object> adminReviewRecharge(Long id, YcPointsRechargeReviewDTO dto, String authorization) {
        YcUserAccount admin = requireAdmin(authorization);
        if (id == null) {
            throw new CustomException("400", "充值单ID不能为空");
        }
        YcPointsRechargeOrder order = ycPointsMapper.selectRechargeOrderById(id);
        if (order == null) {
            throw new CustomException("404", "充值单不存在");
        }
        boolean approved = dto != null && Boolean.TRUE.equals(dto.getApproved());
        String nextStatus = approved ? STATUS_APPROVED : STATUS_REJECTED;
        int affected = ycPointsMapper.reviewRecharge(id, nextStatus, admin.getId(), trimToNull(dto == null ? null : dto.getRemark()));
        if (affected <= 0) {
            throw new CustomException("409", "充值单不是待审核状态");
        }
        if (approved) {
            ycPointsMapper.addUserPoints(order.getUserId(), order.getPointsAmount());
        }
        return toRechargeView(ycPointsMapper.selectRechargeOrderById(id));
    }

    @Transactional
    public boolean spendForAi3d(YcUserAccount user) {
        if (isUnlimitedPointsUser(user)) {
            return false;
        }
        int affected = ycPointsMapper.spendUserPoints(user.getId(), AI3D_COST_POINTS);
        if (affected <= 0) {
            throw new CustomException("409", "积分不足，生成 AI 3D 需要 " + AI3D_COST_POINTS + " 积分，请先到个人中心充值");
        }
        return true;
    }

    @Transactional
    public void refundAi3d(YcUserAccount user, boolean charged) {
        if (charged && user != null && !isUnlimitedPointsUser(user)) {
            ycPointsMapper.refundUserPoints(user.getId(), AI3D_COST_POINTS);
        }
    }

    @Transactional
    public boolean startAi3dPersist(YcUserAccount user, YcAiModelWork work) {
        boolean unlimited = isUnlimitedPointsUser(user);
        String chargeStatus = unlimited ? "FREE" : "CHARGED";
        int claimed = ycCollectibleMapper.startAiModelPersist(work.getId(), user.getId(), chargeStatus);
        if (claimed <= 0) {
            throw new CustomException("409", "作品状态已变化，请刷新后重试");
        }
        if (unlimited) {
            return false;
        }
        int affected = ycPointsMapper.spendUserPoints(user.getId(), AI3D_PERSIST_COST_POINTS);
        if (affected <= 0) {
            throw new CustomException("409", "积分不足，永久保存需要 " + AI3D_PERSIST_COST_POINTS + " 积分");
        }
        return true;
    }

    @Transactional
    public void refundAi3dPersist(YcUserAccount user, YcAiModelWork work, boolean charged, String errorMessage) {
        String chargeStatus = isUnlimitedPointsUser(user) ? "FREE" : "REFUNDED";
        if (charged && !isUnlimitedPointsUser(user)) {
            ycPointsMapper.refundUserPoints(user.getId(), AI3D_PERSIST_COST_POINTS);
        }
        ycCollectibleMapper.failAiModelPersist(work.getId(), user.getId(), errorMessage, chargeStatus);
    }

    @Transactional
    public boolean refundFailedAi3dGeneration(YcUserAccount user, YcAiModelWork work, String errorMessage) {
        boolean charged = "CHARGED".equals(work.getGenerationChargeStatus()) && !isUnlimitedPointsUser(user);
        String chargeStatus = isUnlimitedPointsUser(user) ? "FREE" : (charged ? "REFUNDED" : work.getGenerationChargeStatus());
        int claimed = ycCollectibleMapper.failAiModelGeneration(
                work.getId(),
                user.getId(),
                errorMessage,
                chargeStatus
        );
        if (claimed <= 0) {
            return false;
        }
        if (charged) {
            ycPointsMapper.refundUserPoints(user.getId(), valueOrDefault(work.getGenerationPointsCost(), AI3D_COST_POINTS));
        }
        return true;
    }

    @Transactional
    public boolean recoverStaleAi3dPersist(YcAiModelWork work, LocalDateTime cutoff) {
        boolean charged = "CHARGED".equals(work.getPersistChargeStatus());
        String chargeStatus = charged ? "REFUNDED" : "FREE";
        int claimed = ycCollectibleMapper.failStaleAiModelPersist(
                work.getId(),
                work.getUserId(),
                cutoff,
                chargeStatus,
                "永久保存超时，已自动恢复"
        );
        if (claimed <= 0) {
            return false;
        }
        if (charged) {
            ycPointsMapper.refundUserPoints(work.getUserId(), AI3D_PERSIST_COST_POINTS);
        }
        return true;
    }

    private Map<String, Object> toSummary(YcUserAccount user) {
        YcUserAccount fresh = ycCollectibleMapper.selectUserById(user.getId());
        if (fresh == null) {
            fresh = user;
        }
        boolean unlimited = isUnlimitedPointsUser(fresh);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("balance", unlimited ? null : valueOrZero(fresh.getPointsBalance()));
        result.put("displayBalance", unlimited ? "无限" : String.valueOf(valueOrZero(fresh.getPointsBalance())));
        result.put("totalRecharged", valueOrZero(fresh.getPointsTotalRecharged()));
        result.put("totalSpent", valueOrZero(fresh.getPointsTotalSpent()));
        result.put("unlimited", unlimited);
        result.put("ai3dCost", AI3D_COST_POINTS);
        result.put("ai3dPersistCost", AI3D_PERSIST_COST_POINTS);
        result.put("rechargeRate", RECHARGE_POINTS_PER_YUAN);
        result.put("paymentQrUrl", trimToNull(paymentQrUrl) == null ? "/picket.webp" : paymentQrUrl.trim());
        return result;
    }

    private Map<String, Object> toRechargeView(YcPointsRechargeOrder order) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (order == null) {
            return map;
        }
        map.put("id", order.getId());
        map.put("rechargeNo", order.getRechargeNo());
        map.put("userId", order.getUserId());
        map.put("username", order.getUsername());
        map.put("displayName", order.getDisplayName());
        map.put("amount", order.getAmount());
        map.put("pointsAmount", order.getPointsAmount());
        map.put("status", order.getStatus());
        map.put("statusLabel", resolveStatusLabel(order.getStatus()));
        map.put("paymentMarkedAt", order.getPaymentMarkedAt());
        map.put("paymentReviewedAt", order.getPaymentReviewedAt());
        map.put("paymentReviewBy", order.getPaymentReviewBy());
        map.put("paymentReviewByName", order.getPaymentReviewByName());
        map.put("paymentReviewRemark", order.getPaymentReviewRemark());
        map.put("createdAt", order.getCreatedAt());
        map.put("updatedAt", order.getUpdatedAt());
        return map;
    }

    private String resolveStatusLabel(String status) {
        if (STATUS_PENDING_PAYMENT.equals(status)) return "待付款";
        if (STATUS_PENDING_REVIEW.equals(status)) return "待审核";
        if (STATUS_APPROVED.equals(status)) return "已到账";
        if (STATUS_REJECTED.equals(status)) return "审核未通过";
        return "未知";
    }

    private YcUserAccount requireLogin(String authorization) {
        if (isBlank(authorization) || !authorization.startsWith("Bearer ")) {
            throw new CustomException("401", "请先登录");
        }
        String token = authorization.substring(7).trim();
        if (!jwtUtil.validateToken(token)) {
            throw new CustomException("401", "登录状态已失效");
        }
        String username = jwtUtil.getUsernameFromToken(token);
        YcUserAccount user = ycCollectibleMapper.selectUserByUsername(username);
        if (user == null) {
            throw new CustomException("401", "用户不存在");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new CustomException("403", "账号已禁用");
        }
        return user;
    }

    private YcUserAccount requireAdmin(String authorization) {
        YcUserAccount user = requireLogin(authorization);
        if (!"admin".equals(user.getRole())) {
            throw new CustomException("403", "仅管理员可操作");
        }
        return user;
    }

    private boolean isUnlimitedPointsUser(YcUserAccount user) {
        return user != null && (
                (user.getPointsIsUnlimited() != null && user.getPointsIsUnlimited() == 1)
                        || "admin".equalsIgnoreCase(user.getRole())
                        || "ycadmin".equalsIgnoreCase(user.getUsername())
        );
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CustomException("400", "充值金额必须大于0");
        }
        BigDecimal normalized = amount.setScale(2, RoundingMode.HALF_UP);
        if (normalized.compareTo(new BigDecimal("9999.00")) > 0) {
            throw new CustomException("400", "单次充值金额不能超过9999元");
        }
        return normalized;
    }

    private String generateRechargeNo(Long userId) {
        String time = LocalDateTime.now().format(ORDER_NO_TIME);
        long suffix = System.currentTimeMillis() % 100000;
        return "YCP" + time + String.format("%05d", suffix) + String.format("%03d", userId % 1000);
    }

    private int normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private int normalizePageSize(Integer pageSize) {
        return pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private int valueOrDefault(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
