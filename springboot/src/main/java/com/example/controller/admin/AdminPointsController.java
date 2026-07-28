package com.example.controller.admin;

import com.example.common.Result;
import com.example.dto.YcPointsRechargeReviewDTO;
import com.example.service.YcPointsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Map;

@RestController
@RequestMapping("/admin/points")
public class AdminPointsController {

    @Resource
    private YcPointsService ycPointsService;

    @GetMapping("/recharges")
    public Result<Map<String, Object>> recharges(@RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) String status,
                                                 @RequestParam(required = false) Integer page,
                                                 @RequestParam(required = false) Integer pageSize,
                                                 @RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.success(ycPointsService.adminListRecharges(keyword, status, page, pageSize, authorization));
    }

    @PostMapping("/recharges/{id}/review")
    public Result<Map<String, Object>> review(@PathVariable Long id,
                                              @RequestBody YcPointsRechargeReviewDTO dto,
                                              @RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.success(ycPointsService.adminReviewRecharge(id, dto, authorization));
    }
}
