package com.example.service;

import com.example.common.UploadUtil;
import com.example.common.JwtUtil;
import com.example.dto.YcAdminCreateItemDTO;
import com.example.dto.YcAdminUpdateItemDTO;
import com.example.dto.YcCommunityPostCreateDTO;
import com.example.dto.YcCommunityPostUpdateDTO;
import com.example.dto.YcCreateRedeemCodeDTO;
import com.example.dto.YcKilnGuideConfigUpdateDTO;
import com.example.dto.YcKilnGuideHotspotDTO;
import com.example.dto.YcKilnGuideStepDTO;
import com.example.dto.YcLoginDTO;
import com.example.dto.YcRegisterDTO;
import com.example.dto.YcShelfUpdateDTO;
import com.example.entity.YcAiModelWork;
import com.example.entity.YcCollectionItem;
import com.example.entity.YcCollectionSeries;
import com.example.entity.YcCommunityPost;
import com.example.entity.YcKilnHotspotConfig;
import com.example.entity.YcKilnTimelineStepConfig;
import com.example.entity.YcRedeemCode;
import com.example.entity.YcUserAccount;
import com.example.exception.CustomException;
import com.example.mapper.YcCollectibleMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class YcCollectibleService {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final List<String> ALLOWED_VIEW_KEYS = List.of("outside", "inside", "overhead", "timeline");
    private static final int POST_TITLE_MAX = 120;
    private static final int POST_SUMMARY_MAX = 280;
    private static final int POST_TAG_MAX = 8;
    private static final int POST_IMAGE_MAX = 9;

    @Resource
    private YcCollectibleMapper ycCollectibleMapper;

    @Resource
    private BCryptPasswordEncoder passwordEncoder;

    @Resource
    private JwtUtil jwtUtil;

    @Resource
    private LoginCaptchaService loginCaptchaService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public void register(YcRegisterDTO dto) {
        if (dto == null || isBlank(dto.getUsername()) || isBlank(dto.getPassword())) {
            throw new CustomException("400", "用户名和密码不能为空");
        }
        if (dto.getUsername().length() < 3 || dto.getUsername().length() > 64) {
            throw new CustomException("400", "用户名长度需在3-64之间");
        }
        if (dto.getPassword().length() < 6) {
            throw new CustomException("400", "密码长度不能小于6位");
        }
        if (ycCollectibleMapper.selectUserByUsername(dto.getUsername()) != null) {
            throw new CustomException("409", "用户名已存在");
        }

        YcUserAccount userAccount = new YcUserAccount();
        userAccount.setUsername(dto.getUsername().trim());
        userAccount.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        userAccount.setDisplayName(isBlank(dto.getDisplayName()) ? dto.getUsername().trim() : dto.getDisplayName().trim());
        userAccount.setRole("user");
        userAccount.setStatus(1);
        ycCollectibleMapper.insertUserAccount(userAccount);
    }

    public Map<String, Object> login(YcLoginDTO dto, String clientIp) {
        if (dto == null || isBlank(dto.getUsername()) || isBlank(dto.getPassword())) {
            throw new CustomException("400", "用户名和密码不能为空");
        }
        loginCaptchaService.consume(dto.getCaptchaToken(), clientIp);

        YcUserAccount userAccount = ycCollectibleMapper.selectUserByUsername(dto.getUsername().trim());
        if (userAccount == null || !passwordEncoder.matches(dto.getPassword(), userAccount.getPasswordHash())) {
            throw new CustomException("401", "用户名或密码错误");
        }
        if (userAccount.getStatus() == null || userAccount.getStatus() != 1) {
            throw new CustomException("403", "账号已禁用");
        }

        String token = jwtUtil.generateToken(userAccount.getUsername());
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("user", toSafeUser(userAccount));
        return data;
    }

    public Map<String, Object> currentUser(String authorization) {
        YcUserAccount userAccount = requireLogin(authorization);
        return toSafeUser(userAccount);
    }

    public List<YcCollectionSeries> listSeries() {
        return ycCollectibleMapper.selectEnabledSeries();
    }

    public Map<String, Object> listItems(Long seriesId,
                                         String keyword,
                                         Integer page,
                                         Integer pageSize,
                                         String authorization) {
        int p = normalizePage(page);
        int ps = normalizePageSize(pageSize);
        int offset = (p - 1) * ps;
        Long userId = resolveUserIdOptional(authorization);

        int total = ycCollectibleMapper.countOnShelfItems(seriesId, normalizeKeyword(keyword));
        List<YcCollectionItem> items = ycCollectibleMapper.selectOnShelfItems(seriesId, normalizeKeyword(keyword), userId, offset, ps);

        Map<String, Object> pagination = new HashMap<>();
        pagination.put("page", p);
        pagination.put("pageSize", ps);
        pagination.put("total", total);

        Map<String, Object> result = new HashMap<>();
        result.put("list", items.stream().map(this::toItemView).collect(Collectors.toList()));
        result.put("pagination", pagination);
        return result;
    }

    public Map<String, Object> getItemDetail(Long id, String authorization) {
        if (id == null) {
            throw new CustomException("400", "藏品ID不能为空");
        }
        Long userId = resolveUserIdOptional(authorization);
        YcCollectionItem item = ycCollectibleMapper.selectOnShelfItemDetail(id, userId);
        if (item == null) {
            throw new CustomException("404", "藏品不存在或已下架");
        }
        return toItemView(item);
    }

    @Transactional
    public void favorite(Long itemId, String source, String authorization) {
        requireLogin(authorization);
        throw new CustomException("403", "数字藏品仅支持通过兑换码获取");
    }

    @Transactional
    public void unfavorite(Long itemId, String authorization) {
        requireLogin(authorization);
        throw new CustomException("403", "数字藏品仅支持通过兑换码获取");
    }

    public Map<String, Object> myCollections(Integer page, Integer pageSize, String authorization) {
        YcUserAccount userAccount = requireLogin(authorization);
        int p = normalizePage(page);
        int ps = normalizePageSize(pageSize);
        int offset = (p - 1) * ps;

        int total = ycCollectibleMapper.countUserCollections(userAccount.getId());
        List<YcCollectionItem> items = ycCollectibleMapper.selectUserCollections(userAccount.getId(), offset, ps);

        Map<String, Object> pagination = new HashMap<>();
        pagination.put("page", p);
        pagination.put("pageSize", ps);
        pagination.put("total", total);

        Map<String, Object> result = new HashMap<>();
        result.put("list", items.stream().map(this::toItemView).collect(Collectors.toList()));
        result.put("pagination", pagination);
        return result;
    }

    @Transactional
    public Map<String, Object> redeem(String code, String authorization) {
        YcUserAccount userAccount = requireLogin(authorization);
        if (isBlank(code)) {
            throw new CustomException("400", "兑换码不能为空");
        }

        String normalizedCode = code.trim();
        YcRedeemCode redeemCode = ycCollectibleMapper.selectRedeemCodeForUpdate(normalizedCode);
        if (redeemCode == null) {
            throw new CustomException("404", "兑换码不存在");
        }

        LocalDateTime now = LocalDateTime.now();
        if (redeemCode.getStatus() != null && redeemCode.getStatus() == 1
                && redeemCode.getExpireAt() != null && now.isAfter(redeemCode.getExpireAt())) {
            ycCollectibleMapper.updateRedeemCodeStatus(redeemCode.getId(), 4, null, null);
            throw new CustomException("410", "兑换码已过期");
        }
        if (redeemCode.getStatus() == null || redeemCode.getStatus() != 1) {
            if (redeemCode.getStatus() != null && redeemCode.getStatus() == 2) {
                throw new CustomException("409", "兑换码已被使用");
            }
            if (redeemCode.getStatus() != null && redeemCode.getStatus() == 4) {
                throw new CustomException("410", "兑换码已过期");
            }
            throw new CustomException("409", "兑换码不可用");
        }

        YcCollectionItem item = requireAcquirableItem(redeemCode.getItemId());
        if (ycCollectibleMapper.countUserOwnedItem(userAccount.getId(), item.getId()) > 0) {
            throw new CustomException("409", "你已拥有该藏品，无需重复兑换");
        }

        try {
            ycCollectibleMapper.insertUserCollection(userAccount.getId(), item.getId(),
                    "redeem", redeemCode.getIssuedChannel(), redeemCode.getCode());
        } catch (DuplicateKeyException ex) {
            throw new CustomException("409", "你已拥有该藏品，无需重复兑换");
        }
        ycCollectibleMapper.updateRedeemCodeStatus(redeemCode.getId(), 2, userAccount.getId(), now);
        item.setCollected(1);
        item.setAcquiredAt(now);

        Map<String, Object> result = new HashMap<>();
        result.put("code", redeemCode.getCode());
        result.put("item", toItemView(item));
        return result;
    }

    public List<Map<String, Object>> adminListItems(Long seriesId,
                                                    Integer status,
                                                    Integer isOnShelf,
                                                    String authorization) {
        requireAdmin(authorization);
        List<YcCollectionItem> items = ycCollectibleMapper.adminListItems(seriesId, status, isOnShelf);
        return items.stream().map(this::toItemView).collect(Collectors.toList());
    }

    @Transactional
    public Long adminCreateItem(YcAdminCreateItemDTO dto, String authorization) {
        YcUserAccount operator = requireAdmin(authorization);
        if (dto == null) {
            throw new CustomException("400", "请求体不能为空");
        }
        validateCreateItemDTO(dto);
        ensureSeriesExists(dto.getSeriesId());

        YcCollectionItem item = new YcCollectionItem();
        item.setItemCode(dto.getItemCode().trim());
        item.setSeriesId(dto.getSeriesId());
        item.setName(dto.getName().trim());
        item.setRarityLevel(dto.getRarityLevel() == null ? 1 : dto.getRarityLevel());
        item.setCoverUrl(trimToNull(dto.getCoverUrl()));
        item.setModelUrl(dto.getModelUrl().trim());
        item.setModelFormat(isBlank(dto.getModelFormat()) ? "glb" : dto.getModelFormat().trim());
        item.setDescription(trimToNull(dto.getDescription()));
        item.setIsOnShelf(dto.getIsOnShelf() == null ? 1 : dto.getIsOnShelf());
        item.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        item.setCreatedBy(operator.getId());
        item.setUpdatedBy(operator.getId());

        try {
            ycCollectibleMapper.insertCollectionItem(item);
        } catch (DuplicateKeyException ex) {
            throw new CustomException("409", "藏品编码已存在");
        }
        ycCollectibleMapper.insertShelfLog(item.getId(), "create", operator.getId(), "创建藏品");
        return item.getId();
    }

    @Transactional
    public void adminUpdateItem(Long id, YcAdminUpdateItemDTO dto, String authorization) {
        YcUserAccount operator = requireAdmin(authorization);
        if (id == null) {
            throw new CustomException("400", "藏品ID不能为空");
        }
        if (dto == null) {
            throw new CustomException("400", "请求体不能为空");
        }
        YcCollectionItem old = ycCollectibleMapper.selectItemById(id);
        if (old == null) {
            throw new CustomException("404", "藏品不存在");
        }

        YcCollectionItem item = new YcCollectionItem();
        item.setId(id);
        item.setSeriesId(dto.getSeriesId() == null ? old.getSeriesId() : dto.getSeriesId());
        ensureSeriesExists(item.getSeriesId());
        item.setName(isBlank(dto.getName()) ? old.getName() : dto.getName().trim());
        item.setRarityLevel(dto.getRarityLevel() == null ? old.getRarityLevel() : dto.getRarityLevel());
        item.setCoverUrl(dto.getCoverUrl() == null ? old.getCoverUrl() : trimToNull(dto.getCoverUrl()));
        item.setModelUrl(isBlank(dto.getModelUrl()) ? old.getModelUrl() : dto.getModelUrl().trim());
        item.setModelFormat(isBlank(dto.getModelFormat()) ? old.getModelFormat() : dto.getModelFormat().trim());
        item.setDescription(dto.getDescription() == null ? old.getDescription() : trimToNull(dto.getDescription()));
        item.setStatus(dto.getStatus() == null ? old.getStatus() : dto.getStatus());
        item.setUpdatedBy(operator.getId());

        if (isBlank(item.getName()) || isBlank(item.getModelUrl())) {
            throw new CustomException("400", "藏品名称和模型地址不能为空");
        }
        ycCollectibleMapper.updateCollectionItem(item);
        ycCollectibleMapper.insertShelfLog(id, "update", operator.getId(), "更新藏品信息");
    }

    @Transactional
    public void adminUpdateShelf(Long id, YcShelfUpdateDTO dto, String authorization) {
        YcUserAccount operator = requireAdmin(authorization);
        if (id == null) {
            throw new CustomException("400", "藏品ID不能为空");
        }
        if (dto == null || dto.getIsOnShelf() == null || (dto.getIsOnShelf() != 0 && dto.getIsOnShelf() != 1)) {
            throw new CustomException("400", "isOnShelf 只能是0或1");
        }

        YcCollectionItem item = ycCollectibleMapper.selectItemById(id);
        if (item == null) {
            throw new CustomException("404", "藏品不存在");
        }

        ycCollectibleMapper.updateItemShelf(id, dto.getIsOnShelf(), operator.getId());
        String action = dto.getIsOnShelf() == 1 ? "on_shelf" : "off_shelf";
        ycCollectibleMapper.insertShelfLog(id, action, operator.getId(), trimToNull(dto.getRemark()));
    }

    @Transactional
    public void adminDeleteItem(Long id, String authorization) {
        YcUserAccount operator = requireAdmin(authorization);
        if (id == null) {
            throw new CustomException("400", "藏品ID不能为空");
        }

        YcCollectionItem item = ycCollectibleMapper.selectItemById(id);
        if (item == null) {
            throw new CustomException("404", "藏品不存在");
        }
        int ownerCount = ycCollectibleMapper.countCollectionsByItemId(id);
        if (ownerCount > 0) {
            throw new CustomException("409", "该藏品已被用户收藏，不能删除，请先下架");
        }

        int affected = ycCollectibleMapper.softDeleteCollectionItem(id, operator.getId());
        if (affected <= 0) {
            throw new CustomException("404", "藏品不存在或已删除");
        }
        ycCollectibleMapper.insertShelfLog(id, "delete", operator.getId(), "管理员删除藏品");
    }

    @Transactional
    public void adminCreateRedeemCode(YcCreateRedeemCodeDTO dto, String authorization) {
        YcUserAccount operator = requireAdmin(authorization);
        if (dto == null || isBlank(dto.getCode()) || dto.getItemId() == null) {
            throw new CustomException("400", "code和itemId不能为空");
        }
        YcCollectionItem item = ycCollectibleMapper.selectItemById(dto.getItemId());
        if (item == null) {
            throw new CustomException("404", "目标藏品不存在");
        }

        LocalDateTime expireAt = parseDateTime(dto.getExpireAt());
        try {
            YcRedeemCode existing = ycCollectibleMapper.selectRedeemCodeByItemId(dto.getItemId());
            String code = dto.getCode().trim();
            String issuedChannel = trimToNull(dto.getIssuedChannel());
            if (existing == null) {
                ycCollectibleMapper.insertRedeemCode(code, dto.getItemId(), issuedChannel, expireAt);
                ycCollectibleMapper.insertShelfLog(dto.getItemId(), "update", operator.getId(), "配置兑换码：" + code);
                return;
            }
            if (existing.getStatus() != null && existing.getStatus() == 2) {
                throw new CustomException("409", "该藏品兑换码已被使用，不能直接覆盖");
            }
            int nextStatus = existing.getStatus() != null && existing.getStatus() == 0 ? 0 : 1;
            ycCollectibleMapper.updateRedeemCodeMeta(existing.getId(), code, issuedChannel, expireAt, nextStatus);
            ycCollectibleMapper.insertShelfLog(dto.getItemId(), "update", operator.getId(), "更新兑换码：" + code);
        } catch (DuplicateKeyException ex) {
            throw new CustomException("409", "兑换码已存在");
        }
    }

    public List<Map<String, Object>> adminListRedeemCodes(Integer status,
                                                          Long itemId,
                                                          String authorization) {
        requireAdmin(authorization);
        List<YcRedeemCode> codes = ycCollectibleMapper.adminListRedeemCodes(status, itemId);
        if (codes == null || codes.isEmpty()) {
            return Collections.emptyList();
        }
        return codes.stream().map(this::toRedeemCodeView).collect(Collectors.toList());
    }

    @Transactional
    public void adminInvalidateRedeemCode(Long id, String authorization) {
        YcUserAccount operator = requireAdmin(authorization);
        if (id == null) {
            throw new CustomException("400", "兑换码ID不能为空");
        }
        YcRedeemCode redeemCode = ycCollectibleMapper.selectRedeemCodeById(id);
        if (redeemCode == null) {
            throw new CustomException("404", "兑换码不存在");
        }
        if (redeemCode.getStatus() != null && redeemCode.getStatus() == 2) {
            throw new CustomException("409", "兑换码已被使用，不能作废");
        }
        if (redeemCode.getStatus() != null && redeemCode.getStatus() == 4) {
            throw new CustomException("409", "兑换码已过期，无需重复作废");
        }
        if (redeemCode.getStatus() != null && redeemCode.getStatus() == 0) {
            return;
        }
        ycCollectibleMapper.updateRedeemCodeStatus(id, 0, null, null);
        ycCollectibleMapper.insertShelfLog(redeemCode.getItemId(), "update", operator.getId(), "作废兑换码：" + redeemCode.getCode());
    }

    @Transactional
    public void adminActivateRedeemCode(Long id, String authorization) {
        YcUserAccount operator = requireAdmin(authorization);
        if (id == null) {
            throw new CustomException("400", "兑换码ID不能为空");
        }
        YcRedeemCode redeemCode = ycCollectibleMapper.selectRedeemCodeById(id);
        if (redeemCode == null) {
            throw new CustomException("404", "兑换码不存在");
        }
        if (redeemCode.getStatus() != null && redeemCode.getStatus() == 2) {
            throw new CustomException("409", "兑换码已被使用，不能重新生效");
        }
        if (redeemCode.getStatus() != null && redeemCode.getStatus() == 1) {
            return;
        }
        if (redeemCode.getStatus() != null && redeemCode.getStatus() == 4) {
            throw new CustomException("409", "兑换码已过期，请重新生成新的兑换码");
        }
        if (redeemCode.getExpireAt() != null && redeemCode.getExpireAt().isBefore(LocalDateTime.now())) {
            throw new CustomException("409", "兑换码已过期，请重新生成新的兑换码");
        }
        ycCollectibleMapper.updateRedeemCodeStatus(id, 1, null, null);
        ycCollectibleMapper.insertShelfLog(redeemCode.getItemId(), "update", operator.getId(), "重新生效兑换码：" + redeemCode.getCode());
    }

    public List<Map<String, Object>> listLatestCommunityPosts(Integer limit) {
        int size = limit == null ? 5 : Math.max(1, Math.min(limit, 20));
        List<YcCommunityPost> posts = ycCollectibleMapper.selectLatestCommunityPosts(size);
        if (posts == null || posts.isEmpty()) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> result = new ArrayList<>(posts.size());
        for (YcCommunityPost post : posts) {
            List<String> images = ycCollectibleMapper.selectCommunityPostImages(post.getId());
            post.setImageUrls(images == null ? Collections.emptyList() : images);
            result.add(toCommunityPostView(post));
        }
        return result;
    }

    public Map<String, Object> listCommunityPosts(Integer page,
                                                  Integer pageSize,
                                                  String keyword,
                                                  String category,
                                                  String tag) {
        int p = normalizePage(page);
        int ps = pageSize == null ? 10 : Math.max(1, Math.min(pageSize, 50));
        int offset = (p - 1) * ps;
        String normalizedKeyword = normalizeKeyword(keyword);
        String normalizedCategory = trimToNull(category);
        String normalizedTag = trimToNull(tag);

        int total = ycCollectibleMapper.countCommunityPosts(normalizedKeyword, normalizedCategory, normalizedTag);
        List<YcCommunityPost> posts = ycCollectibleMapper.selectCommunityPosts(normalizedKeyword, normalizedCategory, normalizedTag, offset, ps);

        List<Map<String, Object>> list = new ArrayList<>();
        if (posts != null) {
            for (YcCommunityPost post : posts) {
                List<String> images = ycCollectibleMapper.selectCommunityPostImages(post.getId());
                post.setImageUrls(images == null ? Collections.emptyList() : images);
                list.add(toCommunityPostView(post));
            }
        }

        Map<String, Object> pagination = new HashMap<>();
        pagination.put("page", p);
        pagination.put("pageSize", ps);
        pagination.put("total", total);

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("pagination", pagination);
        return result;
    }

    public List<Map<String, Object>> listMyCommunityPosts(Integer limit, String authorization) {
        YcUserAccount userAccount = requireLogin(authorization);
        int size = limit == null ? 20 : Math.max(1, Math.min(limit, 50));
        List<YcCommunityPost> posts = ycCollectibleMapper.selectUserCommunityPosts(userAccount.getId(), size);
        if (posts == null || posts.isEmpty()) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> result = new ArrayList<>(posts.size());
        for (YcCommunityPost post : posts) {
            List<String> images = ycCollectibleMapper.selectCommunityPostImages(post.getId());
            post.setImageUrls(images == null ? Collections.emptyList() : images);
            result.add(toCommunityPostView(post));
        }
        return result;
    }

    public Map<String, Object> getCommunityPostDetail(Long id) {
        if (id == null) {
            throw new CustomException("400", "帖子ID不能为空");
        }
        YcCommunityPost post = ycCollectibleMapper.selectCommunityPostById(id);
        if (post == null) {
            throw new CustomException("404", "帖子不存在或已下线");
        }
        List<String> images = ycCollectibleMapper.selectCommunityPostImages(post.getId());
        post.setImageUrls(images == null ? Collections.emptyList() : images);
        return toCommunityPostView(post);
    }

    public Map<String, Object> getKilnGuideConfig() {
        List<YcKilnHotspotConfig> hotspots = ycCollectibleMapper.selectEnabledKilnHotspots();
        List<YcKilnTimelineStepConfig> steps = ycCollectibleMapper.selectEnabledKilnTimelineSteps();

        List<Map<String, Object>> hotspotViews = hotspots == null
                ? Collections.emptyList()
                : hotspots.stream()
                .sorted(Comparator.comparing(YcKilnHotspotConfig::getSortNo, Comparator.nullsLast(Integer::compareTo)))
                .map(this::toKilnHotspotView)
                .collect(Collectors.toList());
        List<Map<String, Object>> stepViews = steps == null
                ? Collections.emptyList()
                : steps.stream()
                .sorted(Comparator.comparing(YcKilnTimelineStepConfig::getSortNo, Comparator.nullsLast(Integer::compareTo)))
                .map(this::toKilnStepView)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("hotspots", hotspotViews);
        result.put("steps", stepViews);
        return result;
    }

    @Transactional
    public void adminReplaceKilnGuideConfig(YcKilnGuideConfigUpdateDTO dto, String authorization) {
        requireAdmin(authorization);
        if (dto == null) {
            throw new CustomException("400", "请求体不能为空");
        }

        List<YcKilnGuideHotspotDTO> hotspots = dto.getHotspots() == null ? Collections.emptyList() : dto.getHotspots();
        List<YcKilnGuideStepDTO> steps = dto.getSteps() == null ? Collections.emptyList() : dto.getSteps();
        if (hotspots.isEmpty()) {
            throw new CustomException("400", "hotspots 不能为空");
        }
        if (steps.isEmpty()) {
            throw new CustomException("400", "steps 不能为空");
        }

        ycCollectibleMapper.deleteAllKilnTimelineSteps();
        ycCollectibleMapper.deleteAllKilnHotspots();

        int hotspotSort = 1;
        for (YcKilnGuideHotspotDTO item : hotspots) {
            if (item == null || isBlank(item.getHotspotCode()) || isBlank(item.getName()) || isBlank(item.getPosition())) {
                throw new CustomException("400", "hotspotCode/name/position 不能为空");
            }
            YcKilnHotspotConfig config = new YcKilnHotspotConfig();
            config.setHotspotCode(item.getHotspotCode().trim());
            config.setName(item.getName().trim());
            config.setShortLabel(isBlank(item.getShortLabel()) ? item.getName().substring(0, 1) : item.getShortLabel().trim());
            config.setPosition(item.getPosition().trim());
            config.setNormal(isBlank(item.getNormal()) ? "0m 1m 0m" : item.getNormal().trim());
            config.setFocusOrbit(trimToNull(item.getFocusOrbit()));
            config.setFocusTarget(trimToNull(item.getFocusTarget()));
            config.setSummary(trimToNull(item.getSummary()));
            config.setPointsJson(toJsonString(item.getPoints() == null ? Collections.emptyList() : item.getPoints()));
            config.setTemperatureCurveJson(toJsonString(item.getTemperatureCurve() == null ? Collections.emptyList() : item.getTemperatureCurve()));
            config.setSortNo(item.getSortNo() == null ? hotspotSort : item.getSortNo());
            config.setStatus(item.getStatus() == null ? 1 : item.getStatus());
            ycCollectibleMapper.insertKilnHotspot(config);
            hotspotSort += 1;
        }

        int stepSort = 1;
        for (YcKilnGuideStepDTO item : steps) {
            if (item == null || isBlank(item.getStepCode()) || isBlank(item.getTitle()) || isBlank(item.getHotspotCode())) {
                throw new CustomException("400", "stepCode/title/hotspotCode 不能为空");
            }
            YcKilnTimelineStepConfig config = new YcKilnTimelineStepConfig();
            config.setStepCode(item.getStepCode().trim());
            config.setTitle(item.getTitle().trim());
            config.setSummary(trimToNull(item.getSummary()));
            config.setTemperature(trimToNull(item.getTemperature()));
            config.setHotspotCode(item.getHotspotCode().trim());
            config.setViewKey(normalizeViewKey(item.getViewKey()));
            config.setNarration(trimToNull(item.getNarration()));
            config.setDurationMs(item.getDurationMs() == null ? 5000 : Math.max(800, item.getDurationMs()));
            config.setSortNo(item.getSortNo() == null ? stepSort : item.getSortNo());
            config.setStatus(item.getStatus() == null ? 1 : item.getStatus());
            ycCollectibleMapper.insertKilnTimelineStep(config);
            stepSort += 1;
        }
    }

    @Transactional
    public Map<String, Object> createCommunityPost(YcCommunityPostCreateDTO dto, String authorization) {
        YcUserAccount userAccount = requireLogin(authorization);
        if (dto == null || isBlank(dto.getTitle()) || isBlank(dto.getContentHtml())) {
            throw new CustomException("400", "标题和正文不能为空");
        }

        String safeTitle = dto.getTitle().trim();
        if (safeTitle.length() > POST_TITLE_MAX) {
            throw new CustomException("400", "标题长度不能超过120");
        }
        String safeHtml = sanitizeHtml(dto.getContentHtml());
        if (isBlank(stripHtml(safeHtml))) {
            throw new CustomException("400", "正文内容不能为空");
        }

        List<String> imageUrls = normalizeImageUrls(dto.getImageUrls());
        String summary = trimToNull(dto.getSummary());
        if (isBlank(summary)) {
            summary = abbreviate(stripHtml(safeHtml), 120);
        } else if (summary.length() > POST_SUMMARY_MAX) {
            summary = summary.substring(0, POST_SUMMARY_MAX);
        }
        String category = normalizeCategory(dto.getCategory());
        String tags = normalizeTags(dto.getTags());
        validatePublishableAiWork(dto.getAiWorkId(), userAccount.getId());

        YcCommunityPost post = new YcCommunityPost();
        post.setUserId(userAccount.getId());
        post.setAiWorkId(dto.getAiWorkId());
        post.setTitle(safeTitle);
        post.setContentHtml(safeHtml);
        post.setSummary(summary);
        post.setCategory(category);
        post.setTags(tags);
        post.setCoverImage(imageUrls.isEmpty() ? null : imageUrls.get(0));
        post.setStatus(1);
        post.setLikeCount(0);
        post.setCommentCount(0);
        ycCollectibleMapper.insertCommunityPost(post);

        for (int i = 0; i < imageUrls.size(); i++) {
            ycCollectibleMapper.insertCommunityPostImage(post.getId(), imageUrls.get(i), i + 1);
        }

        post.setAuthorName(isBlank(userAccount.getDisplayName()) ? userAccount.getUsername() : userAccount.getDisplayName());
        post.setImageUrls(imageUrls);
        return toCommunityPostView(post);
    }

    @Transactional
    public Map<String, Object> updateCommunityPost(Long id, YcCommunityPostUpdateDTO dto, String authorization) {
        YcUserAccount operator = requireLogin(authorization);
        if (id == null) {
            throw new CustomException("400", "帖子ID不能为空");
        }
        if (dto == null || isBlank(dto.getTitle()) || isBlank(dto.getContentHtml())) {
            throw new CustomException("400", "标题和正文不能为空");
        }

        YcCommunityPost post = ycCollectibleMapper.selectCommunityPostById(id);
        if (post == null) {
            throw new CustomException("404", "帖子不存在或已下线");
        }
        assertCommunityPostPermission(post, operator);

        String safeTitle = dto.getTitle().trim();
        if (safeTitle.length() > POST_TITLE_MAX) {
            throw new CustomException("400", "标题长度不能超过120");
        }
        String safeHtml = sanitizeHtml(dto.getContentHtml());
        if (isBlank(stripHtml(safeHtml))) {
            throw new CustomException("400", "正文内容不能为空");
        }
        List<String> imageUrls = normalizeImageUrls(dto.getImageUrls());
        String summary = trimToNull(dto.getSummary());
        if (isBlank(summary)) {
            summary = abbreviate(stripHtml(safeHtml), 120);
        } else if (summary.length() > POST_SUMMARY_MAX) {
            summary = summary.substring(0, POST_SUMMARY_MAX);
        }

        post.setTitle(safeTitle);
        post.setContentHtml(safeHtml);
        post.setSummary(summary);
        post.setCategory(normalizeCategory(dto.getCategory()));
        post.setTags(normalizeTags(dto.getTags()));
        validatePublishableAiWork(dto.getAiWorkId(), post.getUserId());
        post.setAiWorkId(dto.getAiWorkId());
        post.setCoverImage(imageUrls.isEmpty() ? null : imageUrls.get(0));
        ycCollectibleMapper.updateCommunityPost(post);

        ycCollectibleMapper.deleteCommunityPostImages(post.getId());
        for (int i = 0; i < imageUrls.size(); i++) {
            ycCollectibleMapper.insertCommunityPostImage(post.getId(), imageUrls.get(i), i + 1);
        }

        post.setImageUrls(imageUrls);
        return toCommunityPostView(post);
    }

    @Transactional
    public void deleteCommunityPost(Long id, String authorization) {
        YcUserAccount operator = requireLogin(authorization);
        if (id == null) {
            throw new CustomException("400", "帖子ID不能为空");
        }
        YcCommunityPost post = ycCollectibleMapper.selectCommunityPostById(id);
        if (post == null) {
            throw new CustomException("404", "帖子不存在或已下线");
        }
        assertCommunityPostPermission(post, operator);

        ycCollectibleMapper.softDeleteCommunityPost(id);
        ycCollectibleMapper.deleteCommunityPostImages(id);
    }

    public Map<String, Object> uploadCommunityImage(MultipartFile file, String authorization) {
        requireLogin(authorization);
        if (file == null || file.isEmpty()) {
            throw new CustomException("400", "图片文件不能为空");
        }
        String contentType = trimToNull(file.getContentType());
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new CustomException("400", "仅支持图片上传");
        }
        if (file.getSize() > 8 * 1024 * 1024L) {
            throw new CustomException("400", "图片大小不能超过8MB");
        }

        try {
            String url = UploadUtil.uploadPhotoWall(file);
            Map<String, Object> result = new HashMap<>();
            result.put("url", url);
            return result;
        } catch (IOException ex) {
            throw new CustomException("500", "图片上传失败");
        }
    }

    private YcCollectionItem requireAcquirableItem(Long itemId) {
        if (itemId == null) {
            throw new CustomException("400", "藏品ID不能为空");
        }
        YcCollectionItem item = ycCollectibleMapper.selectItemById(itemId);
        if (item == null) {
            throw new CustomException("404", "藏品不存在");
        }
        if (item.getStatus() == null || item.getStatus() != 1) {
            throw new CustomException("409", "藏品状态不可用");
        }
        if (item.getIsOnShelf() == null || item.getIsOnShelf() != 1) {
            throw new CustomException("409", "藏品已下架，暂不可获取");
        }
        return item;
    }

    private void validateCreateItemDTO(YcAdminCreateItemDTO dto) {
        if (isBlank(dto.getItemCode()) || dto.getSeriesId() == null || isBlank(dto.getName()) || isBlank(dto.getModelUrl())) {
            throw new CustomException("400", "itemCode、seriesId、name、modelUrl不能为空");
        }
        if (dto.getRarityLevel() != null && (dto.getRarityLevel() < 1 || dto.getRarityLevel() > 5)) {
            throw new CustomException("400", "rarityLevel必须在1-5之间");
        }
    }

    private void ensureSeriesExists(Long seriesId) {
        if (seriesId == null || ycCollectibleMapper.selectSeriesById(seriesId) == null) {
            throw new CustomException("404", "藏品系列不存在");
        }
    }

    private Map<String, Object> toSafeUser(YcUserAccount userAccount) {
        Map<String, Object> user = new HashMap<>();
        user.put("id", userAccount.getId());
        user.put("username", userAccount.getUsername());
        user.put("displayName", userAccount.getDisplayName());
        user.put("role", userAccount.getRole());
        user.put("status", userAccount.getStatus());
        user.put("pointsBalance", userAccount.getPointsBalance() == null ? 0 : userAccount.getPointsBalance());
        user.put("pointsTotalRecharged", userAccount.getPointsTotalRecharged() == null ? 0 : userAccount.getPointsTotalRecharged());
        user.put("pointsTotalSpent", userAccount.getPointsTotalSpent() == null ? 0 : userAccount.getPointsTotalSpent());
        user.put("pointsUnlimited", isUnlimitedPointsUser(userAccount));
        user.put("createdAt", userAccount.getCreatedAt());
        return user;
    }

    private boolean isUnlimitedPointsUser(YcUserAccount userAccount) {
        if (userAccount == null) {
            return false;
        }
        return (userAccount.getPointsIsUnlimited() != null && userAccount.getPointsIsUnlimited() == 1)
                || "admin".equalsIgnoreCase(userAccount.getRole())
                || "ycadmin".equalsIgnoreCase(userAccount.getUsername());
    }

    private void validatePublishableAiWork(Long aiWorkId, Long userId) {
        if (aiWorkId == null) {
            return;
        }
        YcAiModelWork work = ycCollectibleMapper.selectAiModelWorkById(aiWorkId, userId);
        if (work == null || !AiModelWorkPolicy.canPublish(work.getStorageStatus())) {
            throw new CustomException("409", "仅可关联已永久保存的本人作品");
        }
    }

    private Map<String, Object> toItemView(YcCollectionItem item) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", item.getId());
        map.put("itemCode", item.getItemCode());
        map.put("seriesId", item.getSeriesId());
        map.put("seriesCode", item.getSeriesCode());
        map.put("seriesName", item.getSeriesName());
        map.put("name", item.getName());
        map.put("rarityLevel", item.getRarityLevel());
        map.put("coverUrl", item.getCoverUrl());
        map.put("modelUrl", item.getModelUrl());
        map.put("modelFormat", item.getModelFormat());
        map.put("description", item.getDescription());
        map.put("isOnShelf", item.getIsOnShelf());
        map.put("status", item.getStatus());
        map.put("collected", item.getCollected() != null && item.getCollected() == 1);
        map.put("acquiredAt", item.getAcquiredAt());
        map.put("createdAt", item.getCreatedAt());
        map.put("updatedAt", item.getUpdatedAt());
        return map;
    }

    private Map<String, Object> toCommunityPostView(YcCommunityPost post) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", post.getId());
        map.put("userId", post.getUserId());
        map.put("aiWorkId", post.getAiWorkId());
        map.put("authorName", post.getAuthorName());
        map.put("title", post.getTitle());
        map.put("contentHtml", post.getContentHtml());
        map.put("summary", post.getSummary());
        map.put("category", post.getCategory());
        map.put("tags", parseTags(post.getTags()));
        map.put("coverImage", post.getCoverImage());
        map.put("imageUrls", post.getImageUrls() == null ? Collections.emptyList() : post.getImageUrls());
        map.put("likeCount", post.getLikeCount() == null ? 0 : post.getLikeCount());
        map.put("commentCount", post.getCommentCount() == null ? 0 : post.getCommentCount());
        map.put("createdAt", post.getCreatedAt());
        map.put("updatedAt", post.getUpdatedAt());
        return map;
    }

    private Map<String, Object> toRedeemCodeView(YcRedeemCode code) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", code.getId());
        map.put("code", code.getCode());
        map.put("itemId", code.getItemId());
        map.put("itemName", code.getItemName());
        map.put("issuedChannel", code.getIssuedChannel());
        map.put("expireAt", code.getExpireAt());
        map.put("status", code.getStatus());
        map.put("statusLabel", resolveRedeemCodeStatusLabel(code.getStatus()));
        map.put("usedByUserId", code.getUsedByUserId());
        map.put("usedByName", code.getUsedByName());
        map.put("usedAt", code.getUsedAt());
        map.put("createdAt", code.getCreatedAt());
        map.put("updatedAt", code.getUpdatedAt());
        return map;
    }

    private Map<String, Object> toKilnHotspotView(YcKilnHotspotConfig config) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", config.getId());
        map.put("hotspotCode", config.getHotspotCode());
        map.put("name", config.getName());
        map.put("shortLabel", config.getShortLabel());
        map.put("position", config.getPosition());
        map.put("normal", config.getNormal());
        map.put("focusOrbit", config.getFocusOrbit());
        map.put("focusTarget", config.getFocusTarget());
        map.put("summary", config.getSummary());
        map.put("points", parseStringList(config.getPointsJson()));
        map.put("temperatureCurve", parseIntegerList(config.getTemperatureCurveJson()));
        map.put("sortNo", config.getSortNo());
        map.put("status", config.getStatus());
        map.put("createdAt", config.getCreatedAt());
        map.put("updatedAt", config.getUpdatedAt());
        return map;
    }

    private String resolveRedeemCodeStatusLabel(Integer status) {
        if (status == null) {
            return "未知";
        }
        if (status == 1) {
            return "待兑换";
        }
        if (status == 2) {
            return "已使用";
        }
        if (status == 4) {
            return "已过期";
        }
        if (status == 0) {
            return "已作废";
        }
        return "未知";
    }

    private Map<String, Object> toKilnStepView(YcKilnTimelineStepConfig config) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", config.getId());
        map.put("stepCode", config.getStepCode());
        map.put("title", config.getTitle());
        map.put("summary", config.getSummary());
        map.put("temperature", config.getTemperature());
        map.put("hotspotCode", config.getHotspotCode());
        map.put("viewKey", config.getViewKey());
        map.put("narration", config.getNarration());
        map.put("durationMs", config.getDurationMs());
        map.put("sortNo", config.getSortNo());
        map.put("status", config.getStatus());
        map.put("createdAt", config.getCreatedAt());
        map.put("updatedAt", config.getUpdatedAt());
        return map;
    }

    private YcUserAccount requireAdmin(String authorization) {
        YcUserAccount userAccount = requireLogin(authorization);
        if (!"admin".equals(userAccount.getRole())) {
            throw new CustomException("403", "仅管理员可操作");
        }
        return userAccount;
    }

    public void assertAdmin(String authorization) {
        requireAdmin(authorization);
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
        YcUserAccount userAccount = ycCollectibleMapper.selectUserByUsername(username);
        if (userAccount == null) {
            throw new CustomException("401", "用户不存在");
        }
        if (userAccount.getStatus() == null || userAccount.getStatus() != 1) {
            throw new CustomException("403", "账号已禁用");
        }
        return userAccount;
    }

    private Long resolveUserIdOptional(String authorization) {
        if (isBlank(authorization) || !authorization.startsWith("Bearer ")) {
            return null;
        }
        String token = authorization.substring(7).trim();
        if (!jwtUtil.validateToken(token)) {
            return null;
        }
        String username = jwtUtil.getUsernameFromToken(token);
        YcUserAccount userAccount = ycCollectibleMapper.selectUserByUsername(username);
        if (userAccount == null || userAccount.getStatus() == null || userAccount.getStatus() != 1) {
            return null;
        }
        return userAccount.getId();
    }

    private LocalDateTime parseDateTime(String value) {
        if (isBlank(value)) {
            return null;
        }
        String normalized = value.trim();
        try {
            return LocalDateTime.parse(normalized, DATETIME_FORMATTER);
        } catch (DateTimeParseException ignore) {
        }
        try {
            return LocalDateTime.parse(normalized);
        } catch (DateTimeParseException ex) {
            throw new CustomException("400", "expireAt格式错误，支持 yyyy-MM-dd HH:mm:ss 或 ISO-8601");
        }
    }

    private int normalizePage(Integer page) {
        if (page == null || page < 1) {
            return 1;
        }
        return page;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 20;
        }
        return Math.min(pageSize, 100);
    }

    private String normalizeKeyword(String keyword) {
        return isBlank(keyword) ? null : keyword.trim();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeViewKey(String viewKey) {
        String normalized = isBlank(viewKey) ? "timeline" : viewKey.trim();
        if (!ALLOWED_VIEW_KEYS.contains(normalized)) {
            return "timeline";
        }
        return normalized;
    }

    private String toJsonString(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (IOException ex) {
            throw new CustomException("400", "导览配置JSON格式错误");
        }
    }

    private List<String> parseStringList(String json) {
        if (isBlank(json)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (IOException ex) {
            return Collections.emptyList();
        }
    }

    private List<Integer> parseIntegerList(String json) {
        if (isBlank(json)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Integer>>() {});
        } catch (IOException ex) {
            return Collections.emptyList();
        }
    }

    private String sanitizeHtml(String html) {
        String raw = String.valueOf(html == null ? "" : html);
        Safelist safelist = Safelist.relaxed()
                .addTags("span", "h1", "h2", "h3", "h4", "h5", "h6")
                .addAttributes(":all", "class")
                .addAttributes("a", "target", "rel")
                .addProtocols("a", "href", "http", "https", "mailto");
        Document.OutputSettings outputSettings = new Document.OutputSettings();
        outputSettings.prettyPrint(false);
        return Jsoup.clean(raw, "", safelist, outputSettings).trim();
    }

    private String stripHtml(String html) {
        return String.valueOf(html == null ? "" : html)
                .replaceAll("(?is)<[^>]*>", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String abbreviate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    private List<String> normalizeImageUrls(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (String value : imageUrls) {
            String url = trimToNull(value);
            if (url == null) {
                continue;
            }
            if (!url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("/")) {
                continue;
            }
            result.add(url);
            if (result.size() >= POST_IMAGE_MAX) {
                break;
            }
        }
        return result;
    }

    private String normalizeCategory(String category) {
        String value = trimToNull(category);
        if (value == null) {
            return "未分类";
        }
        return value.length() > 40 ? value.substring(0, 40) : value;
    }

    private String normalizeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return null;
        }
        List<String> result = new ArrayList<>();
        for (String tag : tags) {
            String value = trimToNull(tag);
            if (value == null) {
                continue;
            }
            if (value.length() > 20) {
                value = value.substring(0, 20);
            }
            if (!result.contains(value)) {
                result.add(value);
            }
            if (result.size() >= POST_TAG_MAX) {
                break;
            }
        }
        return result.isEmpty() ? null : String.join(",", result);
    }

    private List<String> parseTags(String tags) {
        if (isBlank(tags)) {
            return Collections.emptyList();
        }
        String[] chunks = tags.split(",");
        List<String> result = new ArrayList<>();
        for (String chunk : chunks) {
            String value = trimToNull(chunk);
            if (value != null) {
                result.add(value);
            }
        }
        return result;
    }

    private void assertCommunityPostPermission(YcCommunityPost post, YcUserAccount operator) {
        if (post == null || operator == null) {
            throw new CustomException("403", "无权限操作该帖子");
        }
        if ("admin".equals(operator.getRole())) {
            return;
        }
        if (!post.getUserId().equals(operator.getId())) {
            throw new CustomException("403", "仅帖子作者可操作");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
