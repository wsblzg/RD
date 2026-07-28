package com.example.controller.admin;

import com.example.common.Result;
import com.example.dto.YcAdminShopOrderReviewDTO;
import com.example.dto.YcAdminShopOrderShipDTO;
import com.example.dto.YcShopProductUpsertDTO;
import com.example.dto.YcShelfUpdateDTO;
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
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/shop")
public class AdminShopController {

    @Resource
    private YcShopService ycShopService;

    @GetMapping("/products")
    public Result<List<Map<String, Object>>> products(@RequestParam(required = false) String keyword,
                                                      @RequestParam(required = false) Integer status,
                                                      @RequestParam(required = false) Integer isOnShelf,
                                                      @RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.success(ycShopService.adminListProducts(keyword, status, isOnShelf, authorization));
    }

    @PostMapping("/products")
    public Result<Map<String, Object>> createProduct(@RequestBody YcShopProductUpsertDTO dto,
                                                     @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long id = ycShopService.adminCreateProduct(dto, authorization);
        return Result.success(Map.of("id", id));
    }

    @PutMapping("/products/{id}")
    public Result<Void> updateProduct(@PathVariable Long id,
                                      @RequestBody YcShopProductUpsertDTO dto,
                                      @RequestHeader(value = "Authorization", required = false) String authorization) {
        ycShopService.adminUpdateProduct(id, dto, authorization);
        return Result.success();
    }

    @PutMapping("/products/{id}/shelf")
    public Result<Void> updateProductShelf(@PathVariable Long id,
                                           @RequestBody YcShelfUpdateDTO dto,
                                           @RequestHeader(value = "Authorization", required = false) String authorization) {
        ycShopService.adminUpdateProductShelf(id, dto, authorization);
        return Result.success();
    }

    @DeleteMapping("/products/{id}")
    public Result<Void> deleteProduct(@PathVariable Long id,
                                      @RequestHeader(value = "Authorization", required = false) String authorization) {
        ycShopService.adminDeleteProduct(id, authorization);
        return Result.success();
    }

    @PostMapping("/upload-cover")
    public Result<Map<String, Object>> uploadCover(@RequestParam("file") MultipartFile file,
                                                   @RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.success(ycShopService.adminUploadProductCover(file, authorization));
    }

    @GetMapping("/orders")
    public Result<Map<String, Object>> orders(@RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) String status,
                                              @RequestParam(required = false) Integer page,
                                              @RequestParam(required = false) Integer pageSize,
                                              @RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.success(ycShopService.adminListOrders(keyword, status, page, pageSize, authorization));
    }

    @GetMapping("/orders/{id}")
    public Result<Map<String, Object>> orderDetail(@PathVariable Long id,
                                                   @RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.success(ycShopService.adminGetOrderDetail(id, authorization));
    }

    @PostMapping("/orders/{id}/review-payment")
    public Result<Map<String, Object>> reviewPayment(@PathVariable Long id,
                                                     @RequestBody YcAdminShopOrderReviewDTO dto,
                                                     @RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.success(ycShopService.adminReviewOrder(id, dto, authorization));
    }

    @PostMapping("/orders/{id}/ship")
    public Result<Map<String, Object>> shipOrder(@PathVariable Long id,
                                                 @RequestBody YcAdminShopOrderShipDTO dto,
                                                 @RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.success(ycShopService.adminShipOrder(id, dto, authorization));
    }
}
