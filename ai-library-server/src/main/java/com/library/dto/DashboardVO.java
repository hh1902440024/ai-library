package com.library.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 数据看板响应
 */
@Data
public class DashboardVO {

    private Long userCount;
    private Long bookCount;
    /** 借出中数量 */
    private Long borrowingCount;
    /** 逾期数量 */
    private Long overdueCount;
    /** 近 30 天借阅趋势 [{date, count}] */
    private List<Map<String, Object>> trend;
    /** 分类借阅占比 [{name, value}] */
    private List<Map<String, Object>> categoryStats;
    /** 热门图书 Top10 [{title, borrowCount}] */
    private List<Map<String, Object>> hotBooks;
}
