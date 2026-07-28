package com.example.controller.admin;

import com.example.common.UploadUtil;
import com.example.common.Result;
import com.example.dto.YcAdminCreateItemDTO;
import com.example.dto.YcAdminUpdateItemDTO;
import com.example.dto.YcCreateRedeemCodeDTO;
import com.example.dto.YcKilnGuideConfigUpdateDTO;
import com.example.dto.YcShelfUpdateDTO;
import com.example.exception.CustomException;
import com.example.service.YcCollectibleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/collectibles")
public class AdminCollectibleController {

    @Resource
    private YcCollectibleService ycCollectibleService;

    @GetMapping("/items")
    public Result<List<Map<String, Object>>> listItems(@RequestParam(required = false) Long seriesId,
                                                       @RequestParam(required = false) Integer status,
                                                       @RequestParam(required = false) Integer isOnShelf,
                                                       @RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.success(ycCollectibleService.adminListItems(seriesId, status, isOnShelf, authorization));
    }

    @PostMapping("/items")
    public Result<Map<String, Object>> createItem(@RequestBody YcAdminCreateItemDTO dto,
                                                  @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long id = ycCollectibleService.adminCreateItem(dto, authorization);
        return Result.success(Map.of("id", id));
    }

    @PutMapping("/items/{id}")
    public Result<Void> updateItem(@PathVariable Long id,
                                   @RequestBody YcAdminUpdateItemDTO dto,
                                   @RequestHeader(value = "Authorization", required = false) String authorization) {
        ycCollectibleService.adminUpdateItem(id, dto, authorization);
        return Result.success();
    }

    @DeleteMapping("/items/{id}")
    public Result<Void> deleteItem(@PathVariable Long id,
                                   @RequestHeader(value = "Authorization", required = false) String authorization) {
        ycCollectibleService.adminDeleteItem(id, authorization);
        return Result.success();
    }

    @PutMapping("/items/{id}/shelf")
    public Result<Void> updateShelf(@PathVariable Long id,
                                    @RequestBody YcShelfUpdateDTO dto,
                                    @RequestHeader(value = "Authorization", required = false) String authorization) {
        ycCollectibleService.adminUpdateShelf(id, dto, authorization);
        return Result.success();
    }

    @PostMapping("/redeem-codes")
    public Result<Void> createRedeemCode(@RequestBody YcCreateRedeemCodeDTO dto,
                                         @RequestHeader(value = "Authorization", required = false) String authorization) {
        ycCollectibleService.adminCreateRedeemCode(dto, authorization);
        return Result.success();
    }

    @GetMapping("/redeem-codes")
    public Result<List<Map<String, Object>>> listRedeemCodes(@RequestParam(required = false) Integer status,
                                                             @RequestParam(required = false) Long itemId,
                                                             @RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.success(ycCollectibleService.adminListRedeemCodes(status, itemId, authorization));
    }

    @PutMapping("/redeem-codes/{id}/invalidate")
    public Result<Void> invalidateRedeemCode(@PathVariable Long id,
                                             @RequestHeader(value = "Authorization", required = false) String authorization) {
        ycCollectibleService.adminInvalidateRedeemCode(id, authorization);
        return Result.success();
    }

    @PutMapping("/redeem-codes/{id}/activate")
    public Result<Void> activateRedeemCode(@PathVariable Long id,
                                           @RequestHeader(value = "Authorization", required = false) String authorization) {
        ycCollectibleService.adminActivateRedeemCode(id, authorization);
        return Result.success();
    }

    @PostMapping("/upload-glb")
    public Result<Map<String, Object>> uploadGlb(@RequestParam("file") MultipartFile file,
                                                 @RequestHeader(value = "Authorization", required = false) String authorization) {
        ycCollectibleService.assertAdmin(authorization);
        if (file == null || file.isEmpty()) {
            throw new CustomException("400", "上传文件不能为空");
        }
        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase().endsWith(".glb")) {
            throw new CustomException("400", "仅支持 .glb 文件");
        }
        if (file.getSize() > 100 * 1024 * 1024) {
            throw new CustomException("400", "glb文件不能超过100MB");
        }
        try {
            String modelUrl = UploadUtil.uploadCollectibleModel(file);
            return Result.success(Map.of("modelUrl", modelUrl, "modelFormat", "glb"));
        } catch (IOException e) {
            throw new CustomException("500", "上传glb失败");
        }
    }

    @PostMapping("/upload-cover")
    public Result<Map<String, Object>> uploadCover(@RequestParam("file") MultipartFile file,
                                                   @RequestHeader(value = "Authorization", required = false) String authorization) {
        ycCollectibleService.assertAdmin(authorization);
        if (file == null || file.isEmpty()) {
            throw new CustomException("400", "上传文件不能为空");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new CustomException("400", "仅支持图片文件");
        }
        if (file.getSize() > 8 * 1024 * 1024) {
            throw new CustomException("400", "封面图片不能超过8MB");
        }
        try {
            String coverUrl = UploadUtil.uploadCollectibleCover(file);
            return Result.success(Map.of("coverUrl", coverUrl));
        } catch (IOException e) {
            throw new CustomException("500", "上传封面失败");
        }
    }

    @PutMapping("/guide/config")
    public Result<Void> saveGuideConfig(@RequestBody YcKilnGuideConfigUpdateDTO dto,
                                        @RequestHeader(value = "Authorization", required = false) String authorization) {
        ycCollectibleService.adminReplaceKilnGuideConfig(dto, authorization);
        return Result.success();
    }
}
