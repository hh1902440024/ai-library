package com.library.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 图书视图（含分类名，不暴露内部冗余字段）
 */
@Data
public class BookVO {

    private Long id;
    private String isbn;
    private String title;
    private String author;
    private String publisher;
    private Long categoryId;
    private String categoryName;
    private String cover;
    private String description;
    private String tags;
    private Integer totalStock;
    private Integer availableStock;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate publishDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
