package com.example.mapper;

import com.example.entity.YcCollectionItem;
import com.example.entity.YcCollectionSeries;
import com.example.entity.YcAiModelWork;
import com.example.entity.YcCommunityPost;
import com.example.entity.YcKilnHotspotConfig;
import com.example.entity.YcKilnTimelineStepConfig;
import com.example.entity.YcRedeemCode;
import com.example.entity.YcUserAccount;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface YcCollectibleMapper {
    YcUserAccount selectUserByUsername(@Param("username") String username);

    YcUserAccount selectUserById(@Param("id") Long id);

    int insertUserAccount(YcUserAccount userAccount);

    List<YcCollectionSeries> selectEnabledSeries();

    YcCollectionSeries selectSeriesById(@Param("id") Long id);

    int countOnShelfItems(@Param("seriesId") Long seriesId, @Param("keyword") String keyword);

    List<YcCollectionItem> selectOnShelfItems(@Param("seriesId") Long seriesId,
                                              @Param("keyword") String keyword,
                                              @Param("userId") Long userId,
                                              @Param("offset") Integer offset,
                                              @Param("limit") Integer limit);

    YcCollectionItem selectOnShelfItemDetail(@Param("id") Long id, @Param("userId") Long userId);

    YcCollectionItem selectItemById(@Param("id") Long id);

    int countUserOwnedItem(@Param("userId") Long userId, @Param("itemId") Long itemId);

    int insertUserCollection(@Param("userId") Long userId,
                             @Param("itemId") Long itemId,
                             @Param("acquireType") String acquireType,
                             @Param("acquireSource") String acquireSource,
                             @Param("redeemCode") String redeemCode);

    int deleteFavoriteCollection(@Param("userId") Long userId, @Param("itemId") Long itemId);

    List<YcCollectionItem> selectUserCollections(@Param("userId") Long userId,
                                                 @Param("offset") Integer offset,
                                                 @Param("limit") Integer limit);

    int countUserCollections(@Param("userId") Long userId);

    YcRedeemCode selectRedeemCodeForUpdate(@Param("code") String code);

    YcRedeemCode selectRedeemCodeById(@Param("id") Long id);

    YcRedeemCode selectRedeemCodeByItemId(@Param("itemId") Long itemId);

    int updateRedeemCodeStatus(@Param("id") Long id,
                               @Param("status") Integer status,
                               @Param("usedByUserId") Long usedByUserId,
                               @Param("usedAt") LocalDateTime usedAt);

    int updateRedeemCodeMeta(@Param("id") Long id,
                             @Param("code") String code,
                             @Param("issuedChannel") String issuedChannel,
                             @Param("expireAt") LocalDateTime expireAt,
                             @Param("status") Integer status);

    int insertCollectionItem(YcCollectionItem item);

    int updateCollectionItem(YcCollectionItem item);

    int softDeleteCollectionItem(@Param("id") Long id,
                                 @Param("updatedBy") Long updatedBy);

    int countCollectionsByItemId(@Param("itemId") Long itemId);

    int updateItemShelf(@Param("id") Long id,
                        @Param("isOnShelf") Integer isOnShelf,
                        @Param("updatedBy") Long updatedBy);

    int insertShelfLog(@Param("itemId") Long itemId,
                       @Param("action") String action,
                       @Param("operatorUserId") Long operatorUserId,
                       @Param("remark") String remark);

    List<YcCollectionItem> adminListItems(@Param("seriesId") Long seriesId,
                                          @Param("status") Integer status,
                                          @Param("isOnShelf") Integer isOnShelf);

    int insertRedeemCode(@Param("code") String code,
                         @Param("itemId") Long itemId,
                         @Param("issuedChannel") String issuedChannel,
                         @Param("expireAt") LocalDateTime expireAt);

    List<YcRedeemCode> adminListRedeemCodes(@Param("status") Integer status,
                                            @Param("itemId") Long itemId);

    List<YcCommunityPost> selectLatestCommunityPosts(@Param("limit") Integer limit);

    int countCommunityPosts(@Param("keyword") String keyword,
                            @Param("category") String category,
                            @Param("tag") String tag);

    List<YcCommunityPost> selectCommunityPosts(@Param("keyword") String keyword,
                                               @Param("category") String category,
                                               @Param("tag") String tag,
                                               @Param("offset") Integer offset,
                                               @Param("limit") Integer limit);

    List<YcCommunityPost> selectUserCommunityPosts(@Param("userId") Long userId,
                                                   @Param("limit") Integer limit);

    YcCommunityPost selectCommunityPostById(@Param("id") Long id);

    int insertCommunityPost(YcCommunityPost post);

    int updateCommunityPost(YcCommunityPost post);

    int softDeleteCommunityPost(@Param("id") Long id);

    int insertCommunityPostImage(@Param("postId") Long postId,
                                 @Param("imageUrl") String imageUrl,
                                 @Param("sortNo") Integer sortNo);

    int deleteCommunityPostImages(@Param("postId") Long postId);

    List<String> selectCommunityPostImages(@Param("postId") Long postId);

    int insertAiModelWork(YcAiModelWork work);

    YcAiModelWork selectAiModelWorkByTaskId(@Param("taskId") String taskId,
                                            @Param("userId") Long userId);

    List<YcAiModelWork> selectUserAiModelWorks(@Param("userId") Long userId,
                                               @Param("scope") String scope,
                                               @Param("limit") Integer limit);

    YcAiModelWork selectAiModelWorkById(@Param("id") Long id,
                                        @Param("userId") Long userId);

    YcAiModelWork selectPermanentAiModelWorkById(@Param("id") Long id);

    YcAiModelWork selectLatestAiModelWork(@Param("userId") Long userId);

    YcAiModelWork selectActiveAiModelTask(@Param("userId") Long userId);

    YcAiModelWork selectLatestAiModelPreview(@Param("userId") Long userId);

    int updateAiModelGenerationRunning(@Param("id") Long id, @Param("userId") Long userId);

    int completeAiModelGeneration(@Param("id") Long id,
                                  @Param("userId") Long userId,
                                  @Param("modelUrl") String modelUrl,
                                  @Param("coverUrl") String coverUrl,
                                  @Param("modelFormat") String modelFormat,
                                  @Param("generatedAt") LocalDateTime generatedAt,
                                  @Param("expiresAt") LocalDateTime expiresAt);

    int failAiModelGeneration(@Param("id") Long id,
                              @Param("userId") Long userId,
                              @Param("lastError") String lastError,
                              @Param("chargeStatus") String chargeStatus);

    int startAiModelPersist(@Param("id") Long id,
                            @Param("userId") Long userId,
                            @Param("chargeStatus") String chargeStatus);

    int completeAiModelPersist(@Param("id") Long id,
                               @Param("userId") Long userId,
                               @Param("ossUrl") String ossUrl,
                               @Param("coverUrl") String coverUrl,
                               @Param("modelSizeBytes") Long modelSizeBytes);

    int failAiModelPersist(@Param("id") Long id,
                           @Param("userId") Long userId,
                           @Param("lastError") String lastError,
                           @Param("chargeStatus") String chargeStatus);

    List<YcAiModelWork> selectStaleAiModelPersists(@Param("cutoff") LocalDateTime cutoff,
                                                   @Param("limit") Integer limit);

    int failStaleAiModelPersist(@Param("id") Long id,
                                @Param("userId") Long userId,
                                @Param("cutoff") LocalDateTime cutoff,
                                @Param("chargeStatus") String chargeStatus,
                                @Param("lastError") String lastError);

    int deleteExpiredTemporaryAiModelWorks();

    List<YcKilnHotspotConfig> selectEnabledKilnHotspots();

    List<YcKilnTimelineStepConfig> selectEnabledKilnTimelineSteps();

    int deleteAllKilnHotspots();

    int deleteAllKilnTimelineSteps();

    int insertKilnHotspot(YcKilnHotspotConfig config);

    int insertKilnTimelineStep(YcKilnTimelineStepConfig config);
}
