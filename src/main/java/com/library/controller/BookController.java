package com.library.controller;

import com.library.ai.AiChatService;
import com.library.common.Result;
import com.library.dto.BookDTO;
import com.library.dto.BookQuery;
import com.library.dto.BookVO;
import com.library.dto.PageResult;
import com.library.dto.SummaryVO;
import com.library.interceptor.Anonymous;
import com.library.interceptor.RequireAdmin;
import com.library.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 图书接口
 */
@Tag(name = "图书")
@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;
    private final AiChatService aiChatService;

    @Anonymous
    @Operation(summary = "分页查询图书（关键词/分类/可借）")
    @GetMapping
    public Result<PageResult<BookVO>> list(BookQuery query) {
        return Result.ok(bookService.findByQuery(query));
    }

    @Anonymous
    @Operation(summary = "图书详情")
    @GetMapping("/{id}")
    public Result<BookVO> detail(@PathVariable Long id) {
        return Result.ok(bookService.findDetail(id));
    }

    @Anonymous
    @Operation(summary = "热门图书（按借阅量）")
    @GetMapping("/hot")
    public Result<List<BookVO>> hot(@RequestParam(defaultValue = "8") Integer limit) {
        return Result.ok(bookService.findHot(limit));
    }

    @Anonymous
    @Operation(summary = "同分类推荐（详情页）")
    @GetMapping("/{id}/similar")
    public Result<List<BookVO>> similar(@PathVariable Long id, @RequestParam(defaultValue = "6") Integer limit) {
        return Result.ok(bookService.findSimilar(id, limit));
    }

    @RequireAdmin
    @Operation(summary = "新增图书")
    @PostMapping
    public Result<Void> save(@Valid @RequestBody BookDTO dto) {
        bookService.save(dto);
        return Result.ok("新增成功", null);
    }

    @RequireAdmin
    @Operation(summary = "修改图书")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody BookDTO dto) {
        bookService.update(id, dto);
        return Result.ok("修改成功", null);
    }

    @RequireAdmin
    @Operation(summary = "删除图书（有借出中记录时禁止）")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        bookService.delete(id);
        return Result.ok("删除成功", null);
    }

    @RequireAdmin
    @Operation(summary = "AI 生成图书简介与标签")
    @PostMapping("/{id}/summary")
    public Result<SummaryVO> aiSummary(@PathVariable Long id,
                                       @RequestBody(required = false) SummaryRequest request) {
        String points = request == null ? null : request.points();
        return Result.ok(aiChatService.generateSummary(id, points));
    }

    /** AI 摘要可选输入：管理员填写的目录/要点 */
    public record SummaryRequest(String points) {
    }
}
