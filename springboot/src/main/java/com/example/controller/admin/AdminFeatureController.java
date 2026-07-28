package com.example.controller.admin;

import com.example.common.Result;
import com.example.service.YcCollectibleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/features")
public class AdminFeatureController {

    @Resource
    private YcCollectibleService ycCollectibleService;

    @GetMapping
    public Result<List<Map<String, Object>>> listFeatures(@RequestHeader(value = "Authorization", required = false) String authorization) {
        ycCollectibleService.assertAdmin(authorization);
        return Result.success(List.of(
                feature("AI STUDY", "窑火造物", "查看前台 AI 生成、作品展示、用户发布与作品挂载相关页面。", "/ceramics/ai-creation"),
                feature("COLLECTIONS", "数字藏品与 GLB", "维护藏品上新、上下架、GLB 上传、封面与兑换码。", "/ceramics/admin/collectibles"),
                feature("COMMUNITY", "社区文章", "集中搜索、查看、编辑和删除用户发布的社区内容。", "/ceramics/admin/community"),
                feature("SHOP", "商城商品", "维护文创商品和 3D 模型商品的价格、库存、封面与上下架。", "/ceramics/admin/shop/products"),
                feature("ORDERS", "订单审核", "审核用户付款状态，付款通过后录入物流发货信息。", "/ceramics/admin/shop/orders"),
                feature("POINTS", "积分充值审核", "审核用户积分充值付款，审核通过后自动发放积分。", "/ceramics/admin/points")
        ));
    }

    private Map<String, Object> feature(String kicker, String title, String desc, String to) {
        return Map.of(
                "kicker", kicker,
                "title", title,
                "desc", desc,
                "to", to
        );
    }
}
