package com.library.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.library.common.ResultCode;
import com.library.dto.BorrowQuery;
import com.library.dto.BorrowVO;
import com.library.dto.PageResult;
import com.library.entity.BorrowRecord;
import com.library.exception.BizException;
import com.library.mapper.BookMapper;
import com.library.mapper.BorrowRecordMapper;
import com.library.util.ExcelUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 借阅服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BorrowService {

    /** 默认借期 30 天 */
    private static final int BORROW_DAYS = 30;

    private final BorrowRecordMapper borrowRecordMapper;
    private final BookMapper bookMapper;

    /**
     * 借书（登录）：乐观扣减库存 + 创建借阅记录，同一事务防超卖
     */
    @Transactional
    public void borrow(Long userId, Long bookId) {
        if (bookMapper.findById(bookId) == null) {
            throw new BizException(ResultCode.NOT_FOUND, "图书不存在");
        }
        // 逾期影响后续借阅：有逾期未还记录时禁止再借
        if (borrowRecordMapper.countBlockingOverdue(userId) > 0) {
            throw new BizException("您有逾期未归还的图书，请先归还后再借阅");
        }
        // ① 乐观扣减库存：UPDATE ... WHERE available_stock > 0，影响行数=0 即库存不足
        int rows = bookMapper.decreaseStock(bookId);
        if (rows == 0) {
            throw new BizException("库存不足，暂不可借");
        }
        // ② 创建借阅记录（与扣减同一事务，失败整体回滚）
        BorrowRecord record = new BorrowRecord();
        record.setUserId(userId);
        record.setBookId(bookId);
        record.setBorrowTime(LocalDateTime.now());
        record.setDueTime(LocalDateTime.now().plusDays(BORROW_DAYS));
        record.setStatus(0);
        borrowRecordMapper.insert(record);
        log.info("借书成功: userId={}, bookId={}", userId, bookId);
    }

    /**
     * 还书（本人或管理员）：更新记录状态 + 恢复库存，同一事务保证一致
     */
    @Transactional
    public void returnBook(Long userId, Long recordId, boolean isAdmin) {
        BorrowRecord record = borrowRecordMapper.findById(recordId);
        if (record == null) {
            throw new BizException(ResultCode.NOT_FOUND, "借阅记录不存在");
        }
        // 越权防护：只能操作本人记录，管理员除外
        if (!isAdmin && !record.getUserId().equals(userId)) {
            throw new BizException(ResultCode.FORBIDDEN, "无权操作他人借阅记录");
        }
        if (record.getStatus() != 0) {
            throw new BizException("该记录已归还");
        }
        LocalDateTime now = LocalDateTime.now();
        // 逾期归还标记为 2
        int status = now.isAfter(record.getDueTime()) ? 2 : 1;
        record.setReturnTime(now);
        record.setStatus(status);

        BorrowRecord update = new BorrowRecord();
        update.setId(recordId);
        update.setReturnTime(now);
        update.setStatus(status);
        borrowRecordMapper.updateReturn(update);

        bookMapper.increaseStock(record.getBookId());
        log.info("还书成功: recordId={}, status={}", recordId, status);
    }

    /** 我的借阅：强制按 token 中 userId 过滤，杜绝越权 */
    public PageResult<BorrowVO> myBorrows(Long userId, BorrowQuery query) {
        query.setUserId(userId);
        PageHelper.startPage(query.getPage(), query.getSize());
        List<BorrowVO> list = borrowRecordMapper.findByQuery(query);
        return PageResult.of(new PageInfo<>(list).getTotal(), list);
    }

    /** 管理员：全部借阅记录（多条件） */
    public PageResult<BorrowVO> adminList(BorrowQuery query) {
        PageHelper.startPage(query.getPage(), query.getSize());
        List<BorrowVO> list = borrowRecordMapper.findByQuery(query);
        return PageResult.of(new PageInfo<>(list).getTotal(), list);
    }

    /** 管理员：导出 Excel（同条件，不分页） */
    public byte[] exportExcel(BorrowQuery query) {
        List<BorrowVO> list = borrowRecordMapper.findForExport(query);
        try {
            return ExcelUtil.exportBorrowRecords(list);
        } catch (Exception e) {
            log.error("导出 Excel 失败", e);
            throw new BizException("导出失败，请稍后再试");
        }
    }

    /** 逾期定时任务：每日 0 点将超期未还记录标记为逾期 */
    @Scheduled(cron = "0 0 0 * * ?")
    public void markOverdueTask() {
        markOverdueNow();
    }

    /** 执行逾期标记（定时任务与管理员手动触发共用），返回标记条数 */
    public int markOverdueNow() {
        int rows = borrowRecordMapper.markOverdue();
        if (rows > 0) {
            log.info("逾期标记完成，共 {} 条记录", rows);
        }
        return rows;
    }
}
