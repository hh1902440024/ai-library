package com.library.mapper;

import com.library.dto.BookVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 数据统计 Mapper（管理员看板）
 */
public interface StatsMapper {

    long countUsers();

    long countBooks();

    /** 各状态借阅数量：传 status */
    long countBorrows(@Param("status") Integer status);

    /** 动态逾期数量：超期未标记（status=0 过应还时间）或已标记未归还 */
    long countOverdue();

    /** 近 30 天借阅趋势：[{date, count}] */
    List<Map<String, Object>> borrowTrend();

    /** 分类借阅占比：[{name, value}] */
    List<Map<String, Object>> categoryBorrowStats();

    /** 热门图书 Top N：[{title, borrowCount}] */
    List<Map<String, Object>> hotBooks(@Param("limit") int limit);
}
