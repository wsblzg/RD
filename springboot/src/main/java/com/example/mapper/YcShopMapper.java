package com.example.mapper;

import com.example.entity.YcShopCartItem;
import com.example.entity.YcShopOrder;
import com.example.entity.YcShopOrderItem;
import com.example.entity.YcShopProduct;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface YcShopMapper {
    int countOnShelfProducts(@Param("keyword") String keyword,
                             @Param("productType") String productType);

    List<YcShopProduct> selectOnShelfProducts(@Param("keyword") String keyword,
                                              @Param("productType") String productType,
                                              @Param("offset") Integer offset,
                                              @Param("limit") Integer limit);

    YcShopProduct selectOnShelfProductById(@Param("id") Long id);

    YcShopProduct selectProductById(@Param("id") Long id);

    YcShopProduct selectProductByCode(@Param("productCode") String productCode);

    List<YcShopProduct> adminListProducts(@Param("keyword") String keyword,
                                          @Param("status") Integer status,
                                          @Param("isOnShelf") Integer isOnShelf);

    int insertProduct(YcShopProduct product);

    int updateProduct(YcShopProduct product);

    int updateProductShelf(@Param("id") Long id,
                           @Param("isOnShelf") Integer isOnShelf,
                           @Param("updatedBy") Long updatedBy);

    int softDeleteProduct(@Param("id") Long id,
                          @Param("updatedBy") Long updatedBy);

    int decrementProductStock(@Param("id") Long id,
                              @Param("quantity") Integer quantity);

    int incrementProductStock(@Param("id") Long id,
                              @Param("quantity") Integer quantity);

    int incrementProductSoldCount(@Param("id") Long id,
                                  @Param("quantity") Integer quantity);

    YcShopCartItem selectCartItemByUserAndProduct(@Param("userId") Long userId,
                                                  @Param("productId") Long productId);

    int insertCartItem(@Param("userId") Long userId,
                       @Param("productId") Long productId,
                       @Param("quantity") Integer quantity);

    int updateCartItemQuantity(@Param("id") Long id,
                               @Param("quantity") Integer quantity);

    int deleteCartItem(@Param("id") Long id,
                       @Param("userId") Long userId);

    YcShopCartItem selectCartItemById(@Param("id") Long id,
                                      @Param("userId") Long userId);

    List<YcShopCartItem> selectUserCartItems(@Param("userId") Long userId);

    List<YcShopCartItem> selectCartItemsByIds(@Param("userId") Long userId,
                                              @Param("cartItemIds") List<Long> cartItemIds);

    int deleteCartItems(@Param("userId") Long userId,
                        @Param("cartItemIds") List<Long> cartItemIds);

    int insertOrder(YcShopOrder order);

    int insertOrderItem(YcShopOrderItem item);

    int countUserOrders(@Param("userId") Long userId);

    List<YcShopOrder> selectUserOrders(@Param("userId") Long userId,
                                       @Param("offset") Integer offset,
                                       @Param("limit") Integer limit);

    YcShopOrder selectUserOrderById(@Param("id") Long id,
                                    @Param("userId") Long userId);

    int countAdminOrders(@Param("keyword") String keyword,
                         @Param("status") String status);

    List<YcShopOrder> selectAdminOrders(@Param("keyword") String keyword,
                                        @Param("status") String status,
                                        @Param("offset") Integer offset,
                                        @Param("limit") Integer limit);

    YcShopOrder selectOrderById(@Param("id") Long id);

    List<YcShopOrderItem> selectOrderItems(@Param("orderId") Long orderId);

    List<YcShopProduct> selectPurchasedProducts(@Param("userId") Long userId,
                                                @Param("statuses") List<String> statuses);

    int countPurchasedProduct(@Param("userId") Long userId,
                              @Param("productId") Long productId,
                              @Param("statuses") List<String> statuses);

    List<YcShopOrder> selectExpiredPendingOrders(@Param("cutoff") LocalDateTime cutoff,
                                                 @Param("limit") Integer limit);

    int markOrderPaid(@Param("id") Long id,
                      @Param("userId") Long userId,
                      @Param("paymentMarkedAt") LocalDateTime paymentMarkedAt);

    int cancelUserOrder(@Param("id") Long id,
                        @Param("userId") Long userId,
                        @Param("fromStatus") String fromStatus,
                        @Param("toStatus") String toStatus,
                        @Param("remark") String remark);

    int cancelOrderByStatus(@Param("id") Long id,
                            @Param("fromStatus") String fromStatus,
                            @Param("toStatus") String toStatus,
                            @Param("remark") String remark);

    int reviewOrderPayment(@Param("id") Long id,
                           @Param("fromStatus") String fromStatus,
                           @Param("toStatus") String toStatus,
                           @Param("paymentReviewedAt") LocalDateTime paymentReviewedAt,
                           @Param("paymentReviewBy") Long paymentReviewBy,
                           @Param("paymentReviewRemark") String paymentReviewRemark);

    int markOrderShipped(@Param("id") Long id,
                         @Param("fromStatus") String fromStatus,
                         @Param("toStatus") String toStatus,
                         @Param("shippedAt") LocalDateTime shippedAt,
                         @Param("shippedBy") Long shippedBy,
                         @Param("shippingCompany") String shippingCompany,
                         @Param("trackingNo") String trackingNo);
}
