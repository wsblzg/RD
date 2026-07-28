package com.example.service;

import com.example.common.UploadUtil;
import com.example.common.JwtUtil;
import com.example.dto.YcAdminShopOrderReviewDTO;
import com.example.dto.YcAdminShopOrderShipDTO;
import com.example.dto.YcShopCartAddDTO;
import com.example.dto.YcShopOrderCreateDTO;
import com.example.dto.YcShopProductUpsertDTO;
import com.example.dto.YcShelfUpdateDTO;
import com.example.entity.YcShopCartItem;
import com.example.entity.YcShopOrder;
import com.example.entity.YcShopOrderItem;
import com.example.entity.YcShopProduct;
import com.example.entity.YcUserAccount;
import com.example.exception.CustomException;
import com.example.mapper.YcCollectibleMapper;
import com.example.mapper.YcShopMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class YcShopService {

    private static final String STATUS_PENDING_PAYMENT = "PENDING_PAYMENT";
    private static final String STATUS_PENDING_REVIEW = "PENDING_REVIEW";
    private static final String STATUS_PAYMENT_REJECTED = "PAYMENT_REJECTED";
    private static final String STATUS_PAID = "PAID";
    private static final String STATUS_SHIPPED = "SHIPPED";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final List<String> MODEL_UNLOCK_STATUSES = List.of(STATUS_PAID, STATUS_SHIPPED);
    private static final DateTimeFormatter ORDER_NO_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final int EXPIRE_SCAN_LIMIT = 100;

    @Resource
    private YcShopMapper ycShopMapper;

    @Resource
    private YcCollectibleMapper ycCollectibleMapper;

    @Resource
    private JwtUtil jwtUtil;

    @Value("${shop.order.pending-payment-expire-minutes:30}")
    private long pendingPaymentExpireMinutes;

    @Value("${shop.payment.qr-url:/picket.webp}")
    private String paymentQrUrl;

    public Map<String, Object> getPaymentConfig() {
        Map<String, Object> result = new HashMap<>();
        result.put("qrUrl", trimToNull(paymentQrUrl) == null ? "/picket.webp" : paymentQrUrl.trim());
        return result;
    }

    public Map<String, Object> listProducts(String keyword, String productType, Integer page, Integer pageSize) {
        int p = normalizePage(page);
        int ps = normalizePageSize(pageSize);
        int offset = (p - 1) * ps;
        String normalizedProductType = normalizeProductType(productType);
        int total = ycShopMapper.countOnShelfProducts(normalizeKeyword(keyword), normalizedProductType);
        List<YcShopProduct> list = ycShopMapper.selectOnShelfProducts(normalizeKeyword(keyword), normalizedProductType, offset, ps);

        Map<String, Object> pagination = new HashMap<>();
        pagination.put("page", p);
        pagination.put("pageSize", ps);
        pagination.put("total", total);

        Map<String, Object> result = new HashMap<>();
        result.put("list", list.stream().map(this::toProductView).collect(Collectors.toList()));
        result.put("pagination", pagination);
        return result;
    }

    public Map<String, Object> getProductDetail(Long id, String authorization) {
        if (id == null) {
            throw new CustomException("400", "商品ID不能为空");
        }
        YcShopProduct product = ycShopMapper.selectOnShelfProductById(id);
        if (product == null) {
            throw new CustomException("404", "商品不存在或已下架");
        }
        YcUserAccount user = optionalLogin(authorization);
        boolean purchased = user != null && isPurchasedProduct(user.getId(), product.getId());
        return toProductView(product, purchased);
    }

    public Map<String, Object> listPurchasedModels(String authorization) {
        YcUserAccount user = requireLogin(authorization);
        List<YcShopProduct> products = ycShopMapper.selectPurchasedProducts(user.getId(), MODEL_UNLOCK_STATUSES);
        List<Map<String, Object>> modelProducts = products.stream()
                .filter(this::isModelProduct)
                .map(product -> toProductView(product, true))
                .collect(Collectors.toList());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", modelProducts);
        result.put("total", modelProducts.size());
        return result;
    }

    public Map<String, Object> getCart(String authorization) {
        YcUserAccount user = requireLogin(authorization);
        List<YcShopCartItem> items = ycShopMapper.selectUserCartItems(user.getId());
        BigDecimal totalAmount = BigDecimal.ZERO;
        int totalQuantity = 0;
        List<Map<String, Object>> list = new ArrayList<>();
        for (YcShopCartItem item : items) {
            Map<String, Object> view = toCartItemView(item);
            totalAmount = totalAmount.add((BigDecimal) view.get("subtotalAmount"));
            totalQuantity += item.getQuantity() == null ? 0 : item.getQuantity();
            list.add(view);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("totalAmount", scaleMoney(totalAmount));
        result.put("totalQuantity", totalQuantity);
        return result;
    }

    @Transactional
    public Map<String, Object> addCartItem(YcShopCartAddDTO dto, String authorization) {
        YcUserAccount user = requireLogin(authorization);
        if (dto == null || dto.getProductId() == null) {
            throw new CustomException("400", "商品ID不能为空");
        }
        int quantity = normalizePositiveQuantity(dto.getQuantity());
        YcShopProduct product = requireAvailableProduct(dto.getProductId());
        if (product.getStock() == null || product.getStock() < quantity) {
            throw new CustomException("409", "库存不足");
        }

        YcShopCartItem existing = ycShopMapper.selectCartItemByUserAndProduct(user.getId(), dto.getProductId());
        if (existing == null) {
            try {
                ycShopMapper.insertCartItem(user.getId(), dto.getProductId(), quantity);
            } catch (DuplicateKeyException ex) {
                throw new CustomException("409", "购物车写入失败，请重试");
            }
        } else {
            int nextQuantity = existing.getQuantity() + quantity;
            if (product.getStock() < nextQuantity) {
                throw new CustomException("409", "加入后数量超过库存");
            }
            ycShopMapper.updateCartItemQuantity(existing.getId(), nextQuantity);
        }
        return getCart(authorization);
    }

    @Transactional
    public Map<String, Object> updateCartItem(Long id, Integer quantity, String authorization) {
        YcUserAccount user = requireLogin(authorization);
        if (id == null) {
            throw new CustomException("400", "购物车项ID不能为空");
        }
        int nextQuantity = normalizePositiveQuantity(quantity);
        YcShopCartItem item = ycShopMapper.selectCartItemById(id, user.getId());
        if (item == null) {
            throw new CustomException("404", "购物车项不存在");
        }
        YcShopProduct product = requireAvailableProduct(item.getProductId());
        if (product.getStock() == null || product.getStock() < nextQuantity) {
            throw new CustomException("409", "库存不足");
        }
        ycShopMapper.updateCartItemQuantity(id, nextQuantity);
        return getCart(authorization);
    }

    @Transactional
    public void deleteCartItem(Long id, String authorization) {
        YcUserAccount user = requireLogin(authorization);
        if (id == null) {
            throw new CustomException("400", "购物车项ID不能为空");
        }
        int affected = ycShopMapper.deleteCartItem(id, user.getId());
        if (affected <= 0) {
            throw new CustomException("404", "购物车项不存在");
        }
    }

    @Transactional
    public Map<String, Object> createOrder(YcShopOrderCreateDTO dto, String authorization) {
        YcUserAccount user = requireLogin(authorization);
        if (dto == null || dto.getCartItemIds() == null || dto.getCartItemIds().isEmpty()) {
            throw new CustomException("400", "请先选择要结算的购物车商品");
        }
        String receiverName = requireText(dto.getReceiverName(), "收货人不能为空", 40);
        String receiverPhone = requirePhone(dto.getReceiverPhone());
        String receiverAddress = requireText(dto.getReceiverAddress(), "收货地址不能为空", 200);
        String buyerRemark = trimToNull(dto.getBuyerRemark());
        if (buyerRemark != null && buyerRemark.length() > 200) {
            buyerRemark = buyerRemark.substring(0, 200);
        }

        List<Long> cartItemIds = dto.getCartItemIds().stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .collect(Collectors.toList());
        if (cartItemIds.isEmpty()) {
            throw new CustomException("400", "请先选择要结算的购物车商品");
        }

        List<YcShopCartItem> cartItems = ycShopMapper.selectCartItemsByIds(user.getId(), cartItemIds);
        if (cartItems == null || cartItems.isEmpty()) {
            throw new CustomException("404", "选中的购物车商品不存在");
        }
        if (cartItems.size() != cartItemIds.size()) {
            throw new CustomException("409", "部分购物车商品已失效，请刷新后重试");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        int totalQuantity = 0;
        for (YcShopCartItem cartItem : cartItems) {
            YcShopProduct product = requireAvailableProduct(cartItem.getProductId());
            if (product.getStock() == null || product.getStock() < cartItem.getQuantity()) {
                throw new CustomException("409", product.getName() + " 库存不足");
            }
            int affected = ycShopMapper.decrementProductStock(product.getId(), cartItem.getQuantity());
            if (affected <= 0) {
                throw new CustomException("409", product.getName() + " 库存不足");
            }
            totalQuantity += cartItem.getQuantity();
            totalAmount = totalAmount.add(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }

        YcShopOrder order = new YcShopOrder();
        order.setOrderNo(generateOrderNo(user.getId()));
        order.setUserId(user.getId());
        order.setTotalAmount(scaleMoney(totalAmount));
        order.setTotalQuantity(totalQuantity);
        order.setStatus(STATUS_PENDING_PAYMENT);
        order.setReceiverName(receiverName);
        order.setReceiverPhone(receiverPhone);
        order.setReceiverAddress(receiverAddress);
        order.setBuyerRemark(buyerRemark);
        ycShopMapper.insertOrder(order);

        for (YcShopCartItem cartItem : cartItems) {
            YcShopProduct product = ycShopMapper.selectProductById(cartItem.getProductId());
            YcShopOrderItem orderItem = new YcShopOrderItem();
            orderItem.setOrderId(order.getId());
            orderItem.setProductId(product.getId());
            orderItem.setProductCode(product.getProductCode());
            orderItem.setProductName(product.getName());
            orderItem.setProductSubtitle(product.getSubtitle());
            orderItem.setProductCoverUrl(product.getCoverUrl());
            orderItem.setUnitPrice(scaleMoney(product.getPrice()));
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setSubtotalAmount(scaleMoney(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()))));
            ycShopMapper.insertOrderItem(orderItem);
        }

        ycShopMapper.deleteCartItems(user.getId(), cartItemIds);
        return getUserOrderDetail(order.getId(), authorization);
    }

    public Map<String, Object> listMyOrders(Integer page, Integer pageSize, String authorization) {
        YcUserAccount user = requireLogin(authorization);
        int p = normalizePage(page);
        int ps = normalizePageSize(pageSize);
        int offset = (p - 1) * ps;
        int total = ycShopMapper.countUserOrders(user.getId());
        List<YcShopOrder> list = ycShopMapper.selectUserOrders(user.getId(), offset, ps);

        Map<String, Object> pagination = new HashMap<>();
        pagination.put("page", p);
        pagination.put("pageSize", ps);
        pagination.put("total", total);

        Map<String, Object> result = new HashMap<>();
        result.put("list", list.stream().map(this::toOrderSummaryView).collect(Collectors.toList()));
        result.put("pagination", pagination);
        return result;
    }

    public Map<String, Object> getUserOrderDetail(Long id, String authorization) {
        YcUserAccount user = requireLogin(authorization);
        if (id == null) {
            throw new CustomException("400", "订单ID不能为空");
        }
        YcShopOrder order = ycShopMapper.selectUserOrderById(id, user.getId());
        if (order == null) {
            throw new CustomException("404", "订单不存在");
        }
        return buildOrderDetail(order);
    }

    @Transactional
    public Map<String, Object> markOrderPaid(Long id, String authorization) {
        YcUserAccount user = requireLogin(authorization);
        if (id == null) {
            throw new CustomException("400", "订单ID不能为空");
        }
        YcShopOrder order = ycShopMapper.selectUserOrderById(id, user.getId());
        if (order == null) {
            throw new CustomException("404", "订单不存在");
        }
        if (!STATUS_PENDING_PAYMENT.equals(order.getStatus())) {
            throw new CustomException("409", "当前订单状态不支持提交已付款");
        }
        int affected = ycShopMapper.markOrderPaid(id, user.getId(), LocalDateTime.now());
        if (affected <= 0) {
            throw new CustomException("409", "订单状态已变化，请刷新重试");
        }
        return getUserOrderDetail(id, authorization);
    }

    @Transactional
    public Map<String, Object> cancelOrder(Long id, String authorization) {
        YcUserAccount user = requireLogin(authorization);
        if (id == null) {
            throw new CustomException("400", "订单ID不能为空");
        }
        YcShopOrder order = ycShopMapper.selectUserOrderById(id, user.getId());
        if (order == null) {
            throw new CustomException("404", "订单不存在");
        }
        if (!STATUS_PENDING_PAYMENT.equals(order.getStatus())) {
            throw new CustomException("409", "只有待付款订单可以取消");
        }
        int affected = ycShopMapper.cancelUserOrder(id, user.getId(), STATUS_PENDING_PAYMENT, STATUS_CANCELLED, "用户取消订单，库存已释放");
        if (affected <= 0) {
            throw new CustomException("409", "订单状态已变化，请刷新重试");
        }
        restoreOrderStock(id);
        return getUserOrderDetail(id, authorization);
    }

    public List<Map<String, Object>> adminListProducts(String keyword, Integer status, Integer isOnShelf, String authorization) {
        requireAdmin(authorization);
        List<YcShopProduct> list = ycShopMapper.adminListProducts(normalizeKeyword(keyword), status, isOnShelf);
        return list.stream().map(this::toProductView).collect(Collectors.toList());
    }

    public Map<String, Object> adminUploadProductCover(MultipartFile file, String authorization) {
        requireAdmin(authorization);
        if (file == null || file.isEmpty()) {
            throw new CustomException("400", "上传文件不能为空");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new CustomException("400", "仅支持图片文件");
        }
        if (file.getSize() > 8 * 1024 * 1024) {
            throw new CustomException("400", "商品封面图片不能超过8MB");
        }
        try {
            return Map.of("coverUrl", UploadUtil.uploadShopProductCover(file));
        } catch (IOException e) {
            throw new CustomException("500", "上传商品封面失败");
        }
    }

    @Transactional
    public Long adminCreateProduct(YcShopProductUpsertDTO dto, String authorization) {
        YcUserAccount operator = requireAdmin(authorization);
        validateProductUpsertDTO(dto, true);
        if (ycShopMapper.selectProductByCode(dto.getProductCode().trim()) != null) {
            throw new CustomException("409", "商品编码已存在");
        }
        YcShopProduct product = new YcShopProduct();
        fillProduct(product, dto, operator.getId(), true);
        ycShopMapper.insertProduct(product);
        return product.getId();
    }

    @Transactional
    public void adminUpdateProduct(Long id, YcShopProductUpsertDTO dto, String authorization) {
        YcUserAccount operator = requireAdmin(authorization);
        if (id == null) {
            throw new CustomException("400", "商品ID不能为空");
        }
        validateProductUpsertDTO(dto, false);
        YcShopProduct current = ycShopMapper.selectProductById(id);
        if (current == null) {
            throw new CustomException("404", "商品不存在");
        }
        if (!isBlank(dto.getProductCode())) {
            YcShopProduct duplicate = ycShopMapper.selectProductByCode(dto.getProductCode().trim());
            if (duplicate != null && !duplicate.getId().equals(id)) {
                throw new CustomException("409", "商品编码已存在");
            }
        }
        fillProductForUpdate(current, dto, operator.getId());
        ycShopMapper.updateProduct(current);
    }

    @Transactional
    public void adminUpdateProductShelf(Long id, YcShelfUpdateDTO dto, String authorization) {
        YcUserAccount operator = requireAdmin(authorization);
        if (id == null) {
            throw new CustomException("400", "商品ID不能为空");
        }
        if (dto == null || dto.getIsOnShelf() == null || (dto.getIsOnShelf() != 0 && dto.getIsOnShelf() != 1)) {
            throw new CustomException("400", "isOnShelf 只能是0或1");
        }
        YcShopProduct product = ycShopMapper.selectProductById(id);
        if (product == null) {
            throw new CustomException("404", "商品不存在");
        }
        ycShopMapper.updateProductShelf(id, dto.getIsOnShelf(), operator.getId());
    }

    @Transactional
    public void adminDeleteProduct(Long id, String authorization) {
        YcUserAccount operator = requireAdmin(authorization);
        if (id == null) {
            throw new CustomException("400", "商品ID不能为空");
        }
        YcShopProduct product = ycShopMapper.selectProductById(id);
        if (product == null) {
            throw new CustomException("404", "商品不存在");
        }
        ycShopMapper.softDeleteProduct(id, operator.getId());
    }

    public Map<String, Object> adminListOrders(String keyword, String status, Integer page, Integer pageSize, String authorization) {
        requireAdmin(authorization);
        int p = normalizePage(page);
        int ps = normalizePageSize(pageSize);
        int offset = (p - 1) * ps;
        int total = ycShopMapper.countAdminOrders(normalizeKeyword(keyword), trimToNull(status));
        List<YcShopOrder> list = ycShopMapper.selectAdminOrders(normalizeKeyword(keyword), trimToNull(status), offset, ps);

        Map<String, Object> pagination = new HashMap<>();
        pagination.put("page", p);
        pagination.put("pageSize", ps);
        pagination.put("total", total);

        Map<String, Object> result = new HashMap<>();
        result.put("list", list.stream().map(this::toOrderSummaryView).collect(Collectors.toList()));
        result.put("pagination", pagination);
        return result;
    }

    public Map<String, Object> adminGetOrderDetail(Long id, String authorization) {
        requireAdmin(authorization);
        if (id == null) {
            throw new CustomException("400", "订单ID不能为空");
        }
        YcShopOrder order = ycShopMapper.selectOrderById(id);
        if (order == null) {
            throw new CustomException("404", "订单不存在");
        }
        return buildOrderDetail(order);
    }

    @Transactional
    public Map<String, Object> adminReviewOrder(Long id, YcAdminShopOrderReviewDTO dto, String authorization) {
        YcUserAccount operator = requireAdmin(authorization);
        if (id == null) {
            throw new CustomException("400", "订单ID不能为空");
        }
        if (dto == null || dto.getApproved() == null) {
            throw new CustomException("400", "请指定审核结果");
        }
        YcShopOrder order = ycShopMapper.selectOrderById(id);
        if (order == null) {
            throw new CustomException("404", "订单不存在");
        }
        if (!STATUS_PENDING_REVIEW.equals(order.getStatus())) {
            throw new CustomException("409", "当前订单状态不支持审核付款");
        }

        String nextStatus = dto.getApproved() ? STATUS_PAID : STATUS_PAYMENT_REJECTED;
        String remark = trimToNull(dto.getRemark());
        if (remark != null && remark.length() > 200) {
            remark = remark.substring(0, 200);
        }
        int affected = ycShopMapper.reviewOrderPayment(id, STATUS_PENDING_REVIEW, nextStatus, LocalDateTime.now(), operator.getId(), remark);
        if (affected <= 0) {
            throw new CustomException("409", "订单状态已变化，请刷新重试");
        }

        if (dto.getApproved()) {
            List<YcShopOrderItem> items = ycShopMapper.selectOrderItems(id);
            for (YcShopOrderItem item : items) {
                ycShopMapper.incrementProductSoldCount(item.getProductId(), item.getQuantity());
            }
        } else {
            List<YcShopOrderItem> items = ycShopMapper.selectOrderItems(id);
            for (YcShopOrderItem item : items) {
                ycShopMapper.incrementProductStock(item.getProductId(), item.getQuantity());
            }
        }
        return adminGetOrderDetail(id, authorization);
    }

    @Transactional
    @Scheduled(
            fixedDelayString = "${shop.order.expire-scan-ms:600000}",
            initialDelayString = "${shop.order.expire-scan-initial-delay-ms:30000}"
    )
    public void releaseExpiredPendingOrders() {
        long expireMinutes = Math.max(1, pendingPaymentExpireMinutes);
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(expireMinutes);
        List<YcShopOrder> expiredOrders = ycShopMapper.selectExpiredPendingOrders(cutoff, EXPIRE_SCAN_LIMIT);
        for (YcShopOrder order : expiredOrders) {
            int affected = ycShopMapper.cancelOrderByStatus(
                    order.getId(),
                    STATUS_PENDING_PAYMENT,
                    STATUS_CANCELLED,
                    "超过" + expireMinutes + "分钟未付款，系统自动取消并释放库存"
            );
            if (affected > 0) {
                restoreOrderStock(order.getId());
            }
        }
    }

    @Transactional
    public Map<String, Object> adminShipOrder(Long id, YcAdminShopOrderShipDTO dto, String authorization) {
        YcUserAccount operator = requireAdmin(authorization);
        if (id == null) {
            throw new CustomException("400", "订单ID不能为空");
        }
        if (dto == null) {
            throw new CustomException("400", "请求体不能为空");
        }
        String shippingCompany = requireText(dto.getShippingCompany(), "物流公司不能为空", 60);
        String trackingNo = requireText(dto.getTrackingNo(), "物流单号不能为空", 80);
        YcShopOrder order = ycShopMapper.selectOrderById(id);
        if (order == null) {
            throw new CustomException("404", "订单不存在");
        }
        if (!STATUS_PAID.equals(order.getStatus())) {
            throw new CustomException("409", "只有待发货订单才能执行发货");
        }
        int affected = ycShopMapper.markOrderShipped(id, STATUS_PAID, STATUS_SHIPPED, LocalDateTime.now(), operator.getId(), shippingCompany, trackingNo);
        if (affected <= 0) {
            throw new CustomException("409", "订单状态已变化，请刷新重试");
        }
        return adminGetOrderDetail(id, authorization);
    }

    private Map<String, Object> buildOrderDetail(YcShopOrder order) {
        Map<String, Object> result = new LinkedHashMap<>(toOrderSummaryView(order));
        List<YcShopOrderItem> items = ycShopMapper.selectOrderItems(order.getId());
        result.put("items", items.stream().map(this::toOrderItemView).collect(Collectors.toList()));
        return result;
    }

    private void restoreOrderStock(Long orderId) {
        List<YcShopOrderItem> items = ycShopMapper.selectOrderItems(orderId);
        for (YcShopOrderItem item : items) {
            ycShopMapper.incrementProductStock(item.getProductId(), item.getQuantity());
        }
    }

    private void fillProduct(YcShopProduct product, YcShopProductUpsertDTO dto, Long operatorId, boolean create) {
        product.setProductCode(dto.getProductCode().trim());
        product.setName(dto.getName().trim());
        product.setSubtitle(trimToNull(dto.getSubtitle()));
        product.setCoverUrl(trimToNull(dto.getCoverUrl()));
        product.setDetailContent(trimToNull(dto.getDetailContent()));
        product.setPrice(scaleMoney(dto.getPrice()));
        product.setStock(dto.getStock());
        product.setIsOnShelf(dto.getIsOnShelf() == null ? 1 : dto.getIsOnShelf());
        product.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        product.setSortNo(dto.getSortNo() == null ? 0 : dto.getSortNo());
        if (create) {
            product.setCreatedBy(operatorId);
        }
        product.setUpdatedBy(operatorId);
    }

    private void fillProductForUpdate(YcShopProduct product, YcShopProductUpsertDTO dto, Long operatorId) {
        if (!isBlank(dto.getProductCode())) {
            product.setProductCode(dto.getProductCode().trim());
        }
        if (!isBlank(dto.getName())) {
            product.setName(dto.getName().trim());
        }
        if (dto.getSubtitle() != null) {
            product.setSubtitle(trimToNull(dto.getSubtitle()));
        }
        if (dto.getCoverUrl() != null) {
            product.setCoverUrl(trimToNull(dto.getCoverUrl()));
        }
        if (dto.getDetailContent() != null) {
            product.setDetailContent(trimToNull(dto.getDetailContent()));
        }
        if (dto.getPrice() != null) {
            product.setPrice(scaleMoney(dto.getPrice()));
        }
        if (dto.getStock() != null) {
            product.setStock(dto.getStock());
        }
        if (dto.getIsOnShelf() != null) {
            product.setIsOnShelf(dto.getIsOnShelf());
        }
        if (dto.getStatus() != null) {
            product.setStatus(dto.getStatus());
        }
        if (dto.getSortNo() != null) {
            product.setSortNo(dto.getSortNo());
        }
        product.setUpdatedBy(operatorId);
    }

    private void validateProductUpsertDTO(YcShopProductUpsertDTO dto, boolean requireAll) {
        if (dto == null) {
            throw new CustomException("400", "请求体不能为空");
        }
        if (requireAll && isBlank(dto.getProductCode())) {
            throw new CustomException("400", "商品编码不能为空");
        }
        if (requireAll && isBlank(dto.getName())) {
            throw new CustomException("400", "商品名称不能为空");
        }
        if (requireAll && dto.getPrice() == null) {
            throw new CustomException("400", "商品价格不能为空");
        }
        if (requireAll && dto.getStock() == null) {
            throw new CustomException("400", "库存不能为空");
        }
        if (!isBlank(dto.getProductCode()) && dto.getProductCode().trim().length() > 64) {
            throw new CustomException("400", "商品编码长度不能超过64");
        }
        if (!isBlank(dto.getName()) && dto.getName().trim().length() > 120) {
            throw new CustomException("400", "商品名称长度不能超过120");
        }
        if (dto.getPrice() != null && dto.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new CustomException("400", "商品价格必须大于0");
        }
        if (dto.getStock() != null && dto.getStock() < 0) {
            throw new CustomException("400", "库存不能小于0");
        }
        if (dto.getIsOnShelf() != null && dto.getIsOnShelf() != 0 && dto.getIsOnShelf() != 1) {
            throw new CustomException("400", "isOnShelf 只能是0或1");
        }
        if (dto.getStatus() != null && dto.getStatus() != 0 && dto.getStatus() != 1) {
            throw new CustomException("400", "status 只能是0或1");
        }
    }

    private YcShopProduct requireAvailableProduct(Long id) {
        YcShopProduct product = ycShopMapper.selectProductById(id);
        if (product == null) {
            throw new CustomException("404", "商品不存在");
        }
        if (product.getDeletedAt() != null || product.getStatus() == null || product.getStatus() != 1) {
            throw new CustomException("409", "商品状态不可用");
        }
        if (product.getIsOnShelf() == null || product.getIsOnShelf() != 1) {
            throw new CustomException("409", "商品已下架");
        }
        return product;
    }

    private Map<String, Object> toProductView(YcShopProduct product) {
        return toProductView(product, false);
    }

    private Map<String, Object> toProductView(YcShopProduct product, boolean purchased) {
        boolean modelProduct = isModelProduct(product);
        boolean canViewFullModel = modelProduct && purchased;
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", product.getId());
        map.put("productCode", product.getProductCode());
        map.put("name", product.getName());
        map.put("subtitle", product.getSubtitle());
        map.put("coverUrl", product.getCoverUrl());
        map.put("detailContent", stripLockedModelMeta(product.getDetailContent()));
        map.put("price", scaleMoney(product.getPrice()));
        map.put("stock", product.getStock() == null ? 0 : product.getStock());
        map.put("soldCount", product.getSoldCount() == null ? 0 : product.getSoldCount());
        map.put("productType", modelProduct ? "3D_MODEL" : "PHYSICAL");
        map.put("isModelProduct", modelProduct);
        map.put("previewImageUrls", extractPreviewImages(product));
        map.put("purchased", purchased);
        map.put("canViewFullModel", canViewFullModel);
        map.put("modelFormat", "glb");
        map.put("modelUrl", canViewFullModel ? extractModelUrl(product) : "");
        map.put("isOnShelf", product.getIsOnShelf() != null && product.getIsOnShelf() == 1);
        map.put("status", product.getStatus());
        map.put("sortNo", product.getSortNo());
        map.put("createdAt", product.getCreatedAt());
        map.put("updatedAt", product.getUpdatedAt());
        return map;
    }

    private Map<String, Object> toCartItemView(YcShopCartItem item) {
        Map<String, Object> map = new LinkedHashMap<>();
        BigDecimal price = scaleMoney(item.getPrice());
        BigDecimal subtotal = scaleMoney(price.multiply(BigDecimal.valueOf(item.getQuantity())));
        map.put("id", item.getId());
        map.put("productId", item.getProductId());
        map.put("productCode", item.getProductCode());
        map.put("productName", item.getProductName());
        map.put("productSubtitle", item.getProductSubtitle());
        map.put("coverUrl", item.getCoverUrl());
        map.put("price", price);
        map.put("quantity", item.getQuantity());
        map.put("stock", item.getStock() == null ? 0 : item.getStock());
        map.put("isOnShelf", item.getIsOnShelf() != null && item.getIsOnShelf() == 1);
        map.put("status", item.getStatus());
        map.put("subtotalAmount", subtotal);
        map.put("updatedAt", item.getUpdatedAt());
        return map;
    }

    private Map<String, Object> toOrderSummaryView(YcShopOrder order) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", order.getId());
        map.put("orderNo", order.getOrderNo());
        map.put("userId", order.getUserId());
        map.put("username", order.getUsername());
        map.put("displayName", order.getDisplayName());
        map.put("totalAmount", scaleMoney(order.getTotalAmount()));
        map.put("totalQuantity", order.getTotalQuantity());
        map.put("status", order.getStatus());
        map.put("statusLabel", resolveOrderStatusLabel(order.getStatus()));
        map.put("receiverName", order.getReceiverName());
        map.put("receiverPhone", order.getReceiverPhone());
        map.put("receiverAddress", order.getReceiverAddress());
        map.put("buyerRemark", order.getBuyerRemark());
        map.put("paymentMarkedAt", order.getPaymentMarkedAt());
        map.put("paymentReviewedAt", order.getPaymentReviewedAt());
        map.put("paymentReviewBy", order.getPaymentReviewBy());
        map.put("paymentReviewByName", order.getPaymentReviewByName());
        map.put("paymentReviewRemark", order.getPaymentReviewRemark());
        map.put("shippedAt", order.getShippedAt());
        map.put("shippedBy", order.getShippedBy());
        map.put("shippedByName", order.getShippedByName());
        map.put("shippingCompany", order.getShippingCompany());
        map.put("trackingNo", order.getTrackingNo());
        map.put("createdAt", order.getCreatedAt());
        map.put("updatedAt", order.getUpdatedAt());
        return map;
    }

    private Map<String, Object> toOrderItemView(YcShopOrderItem item) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", item.getId());
        map.put("orderId", item.getOrderId());
        map.put("productId", item.getProductId());
        map.put("productCode", item.getProductCode());
        map.put("productName", item.getProductName());
        map.put("productSubtitle", item.getProductSubtitle());
        map.put("productCoverUrl", item.getProductCoverUrl());
        map.put("unitPrice", scaleMoney(item.getUnitPrice()));
        map.put("quantity", item.getQuantity());
        map.put("subtotalAmount", scaleMoney(item.getSubtotalAmount()));
        map.put("createdAt", item.getCreatedAt());
        return map;
    }

    private String resolveOrderStatusLabel(String status) {
        if (STATUS_PENDING_PAYMENT.equals(status)) {
            return "待付款";
        }
        if (STATUS_PENDING_REVIEW.equals(status)) {
            return "待审核";
        }
        if (STATUS_PAYMENT_REJECTED.equals(status)) {
            return "审核未通过";
        }
        if (STATUS_PAID.equals(status)) {
            return "待发货";
        }
        if (STATUS_SHIPPED.equals(status)) {
            return "已发货";
        }
        if (STATUS_CANCELLED.equals(status)) {
            return "已取消";
        }
        return "未知状态";
    }

    private String generateOrderNo(Long userId) {
        String time = LocalDateTime.now().format(ORDER_NO_TIME);
        long suffix = System.currentTimeMillis() % 100000;
        return "YC" + time + String.format("%05d", suffix) + String.format("%03d", userId % 1000);
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

    private YcUserAccount optionalLogin(String authorization) {
        if (isBlank(authorization) || !authorization.startsWith("Bearer ")) {
            return null;
        }
        try {
            return requireLogin(authorization);
        } catch (CustomException ex) {
            return null;
        }
    }

    private YcUserAccount requireAdmin(String authorization) {
        YcUserAccount user = requireLogin(authorization);
        if (!"admin".equals(user.getRole())) {
            throw new CustomException("403", "仅管理员可操作");
        }
        return user;
    }

    private int normalizePage(Integer page) {
        if (page == null || page < 1) {
            return 1;
        }
        return page;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 10;
        }
        return Math.min(pageSize, 100);
    }

    private int normalizePositiveQuantity(Integer quantity) {
        if (quantity == null || quantity < 1) {
            throw new CustomException("400", "数量必须大于0");
        }
        if (quantity > 999) {
            throw new CustomException("400", "单次购买数量不能超过999");
        }
        return quantity;
    }

    private String normalizeKeyword(String keyword) {
        return isBlank(keyword) ? null : keyword.trim();
    }

    private String normalizeProductType(String productType) {
        if (isBlank(productType)) {
            return null;
        }
        String normalized = productType.trim().toLowerCase();
        if ("model".equals(normalized) || "3d".equals(normalized) || "3d_model".equals(normalized)) {
            return "model";
        }
        if ("physical".equals(normalized) || "goods".equals(normalized)) {
            return "physical";
        }
        return null;
    }

    private boolean isPurchasedProduct(Long userId, Long productId) {
        if (userId == null || productId == null) {
            return false;
        }
        return ycShopMapper.countPurchasedProduct(userId, productId, MODEL_UNLOCK_STATUSES) > 0;
    }

    private boolean isModelProduct(YcShopProduct product) {
        if (product == null) {
            return false;
        }
        String text = String.join(" ",
                safeLower(product.getProductCode()),
                safeLower(product.getName()),
                safeLower(product.getSubtitle()),
                safeLower(product.getDetailContent()),
                safeLower(extractModelUrl(product))
        );
        return text.contains("glb")
                || text.contains("gltf")
                || text.contains("3d")
                || text.contains("模型商品")
                || text.contains("数字模型")
                || text.contains("modelurl")
                || text.contains("glburl")
                || text.contains("模型地址");
    }

    private String extractModelUrl(YcShopProduct product) {
        if (product == null) {
            return null;
        }
        String detail = product.getDetailContent();
        String modelUrl = findMetaLineValue(detail, "modelUrl", "fullModelUrl", "glbUrl", "模型地址");
        if (!isBlank(modelUrl)) {
            return modelUrl;
        }
        String cover = trimToNull(product.getCoverUrl());
        if (cover != null && (cover.toLowerCase().endsWith(".glb") || cover.toLowerCase().endsWith(".gltf"))) {
            return cover;
        }
        return null;
    }

    private List<String> extractPreviewImages(YcShopProduct product) {
        List<String> images = new ArrayList<>();
        String meta = findMetaLineValue(product == null ? null : product.getDetailContent(),
                "front", "frontView", "preview", "previewImages", "modelPreviewImages", "threeViewImages", "三视图");
        if (!isBlank(meta)) {
            for (String item : meta.split("[,，|\\n]")) {
                String value = trimToNull(item);
                if (value != null && !images.contains(value)) {
                    images.add(value);
                }
            }
        }
        String cover = trimToNull(product == null ? null : product.getCoverUrl());
        if (images.isEmpty() && cover != null && !cover.toLowerCase().endsWith(".glb") && !cover.toLowerCase().endsWith(".gltf")) {
            images.add(cover);
        }
        return images.stream().limit(1).collect(Collectors.toList());
    }

    private String stripLockedModelMeta(String detailContent) {
        if (isBlank(detailContent)) {
            return detailContent;
        }
        List<String> lines = new ArrayList<>();
        for (String line : detailContent.split("\\R")) {
            String lower = line.trim().toLowerCase();
            if (lower.startsWith("modelurl:")
                    || lower.startsWith("fullmodelurl:")
                    || lower.startsWith("glburl:")
                    || lower.startsWith("modelurl：")
                    || lower.startsWith("fullmodelurl：")
                    || lower.startsWith("glburl：")
                    || line.trim().startsWith("模型地址:")
                    || line.trim().startsWith("模型地址：")) {
                continue;
            }
            lines.add(line);
        }
        return String.join("\n", lines).trim();
    }

    private String findMetaLineValue(String source, String... keys) {
        if (isBlank(source) || keys == null) {
            return null;
        }
        for (String line : source.split("\\R")) {
            String trimmed = line.trim();
            for (String key : keys) {
                if (trimmed.regionMatches(true, 0, key + ":", 0, key.length() + 1)
                        || trimmed.regionMatches(true, 0, key + "：", 0, key.length() + 1)) {
                    return trimToNull(trimmed.substring(key.length() + 1));
                }
            }
        }
        return null;
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase();
    }

    private String requireText(String value, String errorMessage, int maxLength) {
        if (isBlank(value)) {
            throw new CustomException("400", errorMessage);
        }
        String result = value.trim();
        if (result.length() > maxLength) {
            result = result.substring(0, maxLength);
        }
        return result;
    }

    private String requirePhone(String phone) {
        if (isBlank(phone)) {
            throw new CustomException("400", "联系电话不能为空");
        }
        String normalized = phone.trim();
        if (!normalized.matches("^1\\d{10}$")) {
            throw new CustomException("400", "联系电话格式不正确");
        }
        return normalized;
    }

    private BigDecimal scaleMoney(BigDecimal value) {
        BigDecimal source = value == null ? BigDecimal.ZERO : value;
        return source.setScale(2, RoundingMode.HALF_UP);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
