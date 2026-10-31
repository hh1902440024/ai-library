package com.library.controller;

import com.library.common.Result;
import com.library.dto.DashboardVO;
import com.library.interceptor.RequireAdmin;
import com.library.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 数据统计接口（管理员）
 */
@RequireAdmin
@Tag(name = "数据统计")
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @Operation(summary = "看板聚合：指标卡 + 借阅趋势 + 分类占比 + 热门图书")
    @GetMapping("/dashboard")
    public Result<DashboardVO> dashboard() {
        return Result.ok(statsService.dashboard());
    }

    @Operation(summary = "指标卡：用户数/图书数/借出数/逾期数")
    @GetMapping("/overview")
    public Result<Map<String, Long>> overview() {
        return Result.ok(statsService.overview());
    }

    @Operation(summary = "近 30 天借阅趋势")
    @GetMapping("/trend")
    public Result<List<Map<String, Object>>> trend() {
        return Result.ok(statsService.trend());
    }

    @Operation(summary = "分类借阅占比")
    @GetMapping("/category")
    public Result<List<Map<String, Object>>> category() {
        return Result.ok(statsService.categoryStats());
    }

    @Operation(summary = "热门图书 Top10")
    @GetMapping("/hot")
    public Result<List<Map<String, Object>>> hot() {
        return Result.ok(statsService.hotBooks());
    }
}
