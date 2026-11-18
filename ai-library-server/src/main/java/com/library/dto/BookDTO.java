package com.library.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 图书新增/修改请求（管理员）
 */
@Data
public class BookDTO {

    @NotBlank(message = "ISBN 不能为空")
    @Pattern(regexp = "[0-9Xx-]{10,20}", message = "ISBN 格式不正确")
    private String isbn;

    @NotBlank(message = "书名不能为空")
    @Size(max = 128, message = "书名长度不能超过 128")
    private String title;

    @NotBlank(message = "作者不能为空")
    @Size(max = 64, message = "作者长度不能超过 64")
    private String author;

    @Size(max = 64, message = "出版社长度不能超过 64")
    private String publisher;

    @NotNull(message = "请选择分类")
    private Long categoryId;

    private String cover;

    private String description;

    /** 标签，逗号分隔 */
    private String tags;

    @NotNull(message = "总库存不能为空")
    private Integer totalStock;

    private LocalDate publishDate;
}
