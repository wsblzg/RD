package com.example.controller.collectible;

import com.example.common.Result;
import com.example.dto.YcPointsRechargeCreateDTO;
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
@RequestMapping({"/points", "/api/points"})
public class PointsController {

    @Resource
    private YcPointsService ycPointsService;

    @GetMapping("/summary")
    public Result<Map<String, Object>> summary(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.success(ycPointsService.getSummary(authorization));
    }

    @PostMapping("/recharges")
    public Result<Map<String, Object>> createRecharge(@RequestBody YcPointsRechargeCreateDTO dto,
                                                      @RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.success(ycPointsService.createRecharge(dto, authorization));
    }

    @GetMapping("/recharges")
    public Result<Map<String, Object>> myRecharges(@RequestParam(required = false) Integer page,
                                                   @RequestParam(required = false) Integer pageSize,
                                                   @RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.success(ycPointsService.listMyRecharges(page, pageSize, authorization));
    }

    @PostMapping("/recharges/{id}/mark-paid")
    public Result<Map<String, Object>> markPaid(@PathVariable Long id,
                                                @RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.success(ycPointsService.markRechargePaid(id, authorization));
    }
}
