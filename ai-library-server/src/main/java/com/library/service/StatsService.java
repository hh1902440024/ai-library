package com.library.service;

import com.library.dto.DashboardVO;
import com.library.mapper.StatsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据统计服务（管理员看板）
 */
@Service
@RequiredArgsConstructor
public class StatsService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final StatsMapper statsMapper;

    /** 看板聚合数据：指标卡 + 三个图表数据 */
    public DashboardVO dashboard() {
        DashboardVO vo = new DashboardVO();
        Map<String, Long> overview = overview();
        vo.setUserCount(overview.get("userCount"));
        vo.setBookCount(overview.get("bookCount"));
        vo.setBorrowingCount(overview.get("borrowingCount"));
        vo.setOverdueCount(overview.get("overdueCount"));
        vo.setTrend(trend());
        vo.setCategoryStats(categoryStats());
        vo.setHotBooks(hotBooks());
        return vo;
    }

    /** 指标卡：用户数/图书数/借出数/逾期数 */
    public Map<String, Long> overview() {
        Map<String, Long> map = new HashMap<>();
        map.put("userCount", statsMapper.countUsers());
        map.put("bookCount", statsMapper.countBooks());
        map.put("borrowingCount", statsMapper.countBorrows(0));
        map.put("overdueCount", statsMapper.countOverdue());
        return map;
    }

    /** 近 30 天借阅趋势（补零） */
    public List<Map<String, Object>> trend() {
        return fillTrendDays(statsMapper.borrowTrend());
    }

    /** 分类借阅占比 */
    public List<Map<String, Object>> categoryStats() {
        return statsMapper.categoryBorrowStats();
    }

    /** 热门图书 Top10 */
    public List<Map<String, Object>> hotBooks() {
        return statsMapper.hotBooks(10);
    }

    /** 折线图补零：无借阅的日期填 0，保证连续 30 天 */
    private List<Map<String, Object>> fillTrendDays(List<Map<String, Object>> raw) {
        Map<String, Object> byDate = new HashMap<>();
        for (Map<String, Object> row : raw) {
            byDate.put(String.valueOf(row.get("date")), row.get("count"));
        }
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate start = LocalDate.now().minusDays(29);
        for (int i = 0; i < 30; i++) {
            LocalDate day = start.plusDays(i);
            String key = day.format(FMT);
            Map<String, Object> point = new HashMap<>();
            point.put("date", key);
            point.put("count", byDate.getOrDefault(key, 0));
            result.add(point);
        }
        return result;
    }
}
