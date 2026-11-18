package com.library.dto;

import lombok.Data;

/**
 * 分页查询基类：页码从 1 开始
 */
@Data
public class PageQuery {

    private Integer page = 1;
    private Integer size = 10;
}
