package com.library.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 借阅记录视图（关联书名/分类/用户名）
 */
@Data
public class BorrowVO {

    private Long id;
    private Long userId;
    private String username;
    private Long bookId;
    private String bookTitle;
    private String categoryName;

    /** 图书封面地址（可能为空，前端按 ISBN 回退到内置封面） */
    private String bookCover;

    /** 图书 ISBN（前端据此匹配内置封面图） */
    private String bookIsbn;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime borrowTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dueTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime returnTime;

    /** 0 借出 1 已还 2 逾期 */
    private Integer status;

    /** 动态逾期：未标记（status=0）但已超过应还时间 */
    private Boolean overdue;
}
