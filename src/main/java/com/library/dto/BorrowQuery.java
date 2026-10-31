package com.library.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 借阅记录查询（我的借阅 / 管理员全量）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BorrowQuery extends PageQuery {

    /** 0 借出 1 已还 2 逾期 */
    private Integer status;

    /** 管理员按用户筛选（我的借阅接口强制取 token 中的 userId） */
    private Long userId;

    private String keyword;
}
