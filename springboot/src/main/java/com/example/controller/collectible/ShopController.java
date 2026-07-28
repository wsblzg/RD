package com.example.controller.collectible;

import com.example.common.Result;
import com.example.dto.YcShopCartAddDTO;
import com.example.dto.YcShopCartUpdateDTO;
import com.example.dto.YcShopOrderCreateDTO;
import com.example.service.YcShopService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Map;

@RestController
@RequestMapping("/shop")
public class ShopController {

    @Resource
    private YcShopService ycShopService;

    @GetMapping("/products")
    public Result<Map<String, Object>> products(@RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) String productType,
                                                @RequestParam(required = false) Integer page,
                                                @RequestParam(required = false) Integer pageSize) {
        return Result.success(ycShopService.listProducts(keyword, productType, page, pageSize));
    }

    @GetMapping("/products/{id}")
    public Result<Map<String, Object>> productDetail(@PathVariable Long id,
                                                     @RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.success(ycShopService.getProductDetail(id, authorization));
    }

    @GetMapping("/models/purchased")
    public Result<Map<String, Object>> purchasedModels(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.success(ycShopService.listPurchasedModels(authorization));
    }

    @GetMapping("/payment-config")
    public Result<Map<String, Object>> paymentConfig() {
        return Result.success(ycShopService.getPaymentConfig());
    }

    @GetMapping("/cart")
    public Result<Map<String, Object>> cart(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.success(ycShopService.getCart(authorization));
    }

    @PostMapping("/cart/items")
    public Result<Map<String, Object>> addCartItem(@RequestBody YcShopCartAddDTO dto,
                                                   @RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.success(ycShopService.addCartItem(dto, authorization));
    }

    @PutMapping("/cart/items/{id}")
    public Result<Map<String, Object>> updateCartItem(@PathVariable Long id,
                                                      @RequestBody YcShopCartUpdateDTO dto,
                                                      @RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.success(ycShopService.updateCartItem(id, dto == null ? null : dto.getQuantity(), authorization));
    }

    @DeleteMapping("/cart/items/{id}")
    public Result<Void> deleteCartItem(@PathVariable Long id,
                                       @RequestHeader(value = "Authorization", required = false) String authorization) {
        ycShopService.deleteCartItem(id, authorization);
        return Result.success();
    }

    @PostMapping("/orders")
    public Result<Map<String, Object>> createOrder(@RequestBody YcShopOrderCreateDTO dto,
                                                   @RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.success(ycShopService.createOrder(dto, authorization));
    }

    @GetMapping("/orders")
    public Result<Map<String, Object>> myOrders(@RequestParam(required = false) Integer page,
                                                @RequestParam(required = false) Integer pageSize,
                                                @RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.success(ycShopService.listMyOrders(page, pageSize, authorization));
    }

    @GetMapping("/orders/{id}")
    public Result<Map<String, Object>> myOrderDetail(@PathVariable Long id,
                                                     @RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.success(ycShopService.getUserOrderDetail(id, authorization));
    }

    @PostMapping("/orders/{id}/mark-paid")
    public Result<Map<String, Object>> markPaid(@PathVariable Long id,
                                                @RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.success(ycShopService.markOrderPaid(id, authorization));
    }

    @PostMapping("/orders/{id}/cancel")
    public Result<Map<String, Object>> cancelOrder(@PathVariable Long id,
                                                   @RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.success(ycShopService.cancelOrder(id, authorization));
    }
}
