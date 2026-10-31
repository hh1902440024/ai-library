package com.library.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 图书分页查询：keyword（书名/作者/标签模糊）、分类、仅看可借
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BookQuery extends PageQuery {

    private String keyword;

    private Long categoryId;

    /** true = 仅看可借（available_stock > 0） */
    private Boolean available;
}
