package com.example.controller.collectible;

import com.example.common.Result;
import com.example.dto.YcFavoriteDTO;
import com.example.dto.YcCommunityPostCreateDTO;
import com.example.dto.YcCommunityPostUpdateDTO;
import com.example.dto.YcRedeemRequestDTO;
import com.example.entity.YcCollectionSeries;
import com.example.service.YcCollectibleService;
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
@RequestMapping("/collectibles")
public class CollectibleController {

    @Resource
    private YcCollectibleService ycCollectibleService;

    @GetMapping("/series")
    public Result<List<YcCollectionSeries>> series() {
        return Result.success(ycCollectibleService.listSeries());
    }

    @GetMapping("/items")
    public Result<Map<String, Object>> items(@RequestParam(required = false) Long seriesId,
                                             @RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) Integer page,
                                             @RequestParam(required = false) Integer pageSize,
                                             @RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.success(ycCollectibleService.listItems(seriesId, keyword, page, pageSize, authorization));
    }

    @GetMapping("/items/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id,
                                              @RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.success(ycCollectibleService.getItemDetail(id, authorization));
    }

    @PostMapping("/items/{id}/favorite")
    public Result<Void> favorite(@PathVariable Long id,
                                 @RequestBody(required = false) YcFavoriteDTO dto,
                                 @RequestHeader(value = "Authorization", required = false) String authorization) {
        String source = dto == null ? null : dto.getSource();
        ycCollectibleService.favorite(id, source, authorization);
        return Result.success();
    }

    @DeleteMapping("/items/{id}/favorite")
    public Result<Void> unfavorite(@PathVariable Long id,
                                   @RequestHeader(value = "Authorization", required = false) String authorization) {
        ycCollectibleService.unfavorite(id, authorization);
        return Result.success();
    }

    @GetMapping("/me/favorites")
    public Result<Map<String, Object>> myFavorites(@RequestParam(required = false) Integer page,
                                                   @RequestParam(required = false) Integer pageSize,
                                                   @RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.success(ycCollectibleService.myCollections(page, pageSize, authorization));
    }

    @PostMapping("/redeem")
    public Result<Map<String, Object>> redeem(@RequestBody YcRedeemRequestDTO dto,
                                              @RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.success(ycCollectibleService.redeem(dto == null ? null : dto.getCode(), authorization));
    }

    @GetMapping("/community/posts/latest")
    public Result<List<Map<String, Object>>> latestCommunityPosts(@RequestParam(required = false) Integer limit) {
        return Result.success(ycCollectibleService.listLatestCommunityPosts(limit));
    }

    @GetMapping("/community/posts")
    public Result<Map<String, Object>> communityPosts(@RequestParam(required = false) Integer page,
                                                      @RequestParam(required = false) Integer pageSize,
                                                      @RequestParam(required = false) String keyword,
                                                      @RequestParam(required = false) String category,
                                                      @RequestParam(required = false) String tag) {
        return Result.success(ycCollectibleService.listCommunityPosts(page, pageSize, keyword, category, tag));
    }

    @GetMapping("/community/posts/{id}")
    public Result<Map<String, Object>> communityPostDetail(@PathVariable Long id) {
        return Result.success(ycCollectibleService.getCommunityPostDetail(id));
    }

    @GetMapping("/community/posts/me")
    public Result<List<Map<String, Object>>> myCommunityPosts(@RequestParam(required = false) Integer limit,
                                                               @RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.success(ycCollectibleService.listMyCommunityPosts(limit, authorization));
    }

    @PostMapping("/community/posts")
    public Result<Map<String, Object>> createCommunityPost(@RequestBody YcCommunityPostCreateDTO dto,
                                                           @RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.success(ycCollectibleService.createCommunityPost(dto, authorization));
    }

    @PutMapping("/community/posts/{id}")
    public Result<Map<String, Object>> updateCommunityPost(@PathVariable Long id,
                                                           @RequestBody YcCommunityPostUpdateDTO dto,
                                                           @RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.success(ycCollectibleService.updateCommunityPost(id, dto, authorization));
    }

    @DeleteMapping("/community/posts/{id}")
    public Result<Void> deleteCommunityPost(@PathVariable Long id,
                                            @RequestHeader(value = "Authorization", required = false) String authorization) {
        ycCollectibleService.deleteCommunityPost(id, authorization);
        return Result.success();
    }

    @PostMapping("/community/upload-image")
    public Result<Map<String, Object>> uploadCommunityImage(@RequestParam("file") MultipartFile file,
                                                            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.success(ycCollectibleService.uploadCommunityImage(file, authorization));
    }

    @GetMapping("/guide/config")
    public Result<Map<String, Object>> guideConfig() {
        return Result.success(ycCollectibleService.getKilnGuideConfig());
    }
}
