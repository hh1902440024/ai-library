package com.library.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 借阅记录表
 */
@Data
public class BorrowRecord {

    private Long id;
    private Long userId;
    private Long bookId;
    private LocalDateTime borrowTime;
    private LocalDateTime dueTime;
    private LocalDateTime returnTime;
    /** 0 借出 1 已还 2 逾期 */
    private Integer status;
}
