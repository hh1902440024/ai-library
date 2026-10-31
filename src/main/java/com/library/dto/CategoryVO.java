package com.library.dto;

import lombok.Data;

/**
 * 分类视图（含图书数量）
 */
@Data
public class CategoryVO {

    private Long id;
    private String name;
    /** 该分类下图书数量 */
    private Long bookCount;
}
