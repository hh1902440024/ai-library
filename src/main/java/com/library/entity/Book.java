package com.library.entity;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 图书表
 */
@Data
public class Book {

    private Long id;
    private String isbn;
    private String title;
    private String author;
    private String publisher;
    private Long categoryId;
    private String cover;
    private String description;
    /** 标签，逗号分隔 */
    private String tags;
    private Integer totalStock;
    private Integer availableStock;
    private LocalDate publishDate;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
