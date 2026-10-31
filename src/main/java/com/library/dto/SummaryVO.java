package com.library.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * AI 生成图书摘要结果
 */
@Data
@AllArgsConstructor
public class SummaryVO {

    private String description;
    private String tags;
}
