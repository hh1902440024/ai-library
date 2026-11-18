package com.library.mapper;

import com.library.dto.BorrowQuery;
import com.library.dto.BorrowVO;
import com.library.entity.BorrowRecord;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 借阅记录表 Mapper
 */
public interface BorrowRecordMapper {

    int insert(BorrowRecord record);

    BorrowRecord findById(@Param("id") Long id);

    /** 还书：更新归还时间与状态 */
    int updateReturn(BorrowRecord record);

    /** 我的借阅 / 管理员多条件查询（分页由 PageHelper 拦截，联表书名/分类/用户名） */
    List<BorrowVO> findByQuery(BorrowQuery query);

    /** 导出 Excel 用：不带分页的同条件查询 */
    List<BorrowVO> findForExport(BorrowQuery query);

    /** 图书是否有借出中的记录（删除图书前校验） */
    int countActiveByBook(@Param("bookId") Long bookId);

    /** 用户是否有借出中的记录 */
    int countActiveByUser(@Param("userId") Long userId);

    /** 用户逾期未还数量（status=0 超期未标记，或 status=2 未归还）——限制借书 */
    int countBlockingOverdue(@Param("userId") Long userId);

    /** 定时任务：批量标记逾期（status=0 且 due_time < now） */
    int markOverdue();

    /** 用户最近借阅（AI 个性化推荐/偏好分析用，联表书名与分类名） */
    List<BorrowVO> findRecentByUser(@Param("userId") Long userId, @Param("limit") int limit);
}
