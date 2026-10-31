package com.library.controller;

import com.library.common.Result;
import com.library.dto.CategoryVO;
import com.library.interceptor.Anonymous;
import com.library.interceptor.RequireAdmin;
import com.library.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 分类接口
 */
@Tag(name = "分类")
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @Anonymous
    @Operation(summary = "分类列表（含图书数量）")
    @GetMapping
    public Result<List<CategoryVO>> list() {
        return Result.ok(categoryService.findAll());
    }

    @RequireAdmin
    @Operation(summary = "新增分类")
    @PostMapping
    public Result<Void> save(@RequestBody CategoryRequest request) {
        categoryService.save(request.getName());
        return Result.ok("新增成功", null);
    }

    @RequireAdmin
    @Operation(summary = "修改分类")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody CategoryRequest request) {
        categoryService.update(id, request.getName());
        return Result.ok("修改成功", null);
    }

    @RequireAdmin
    @Operation(summary = "删除分类（分类下有图书时禁止）")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.ok("删除成功", null);
    }

    @Data
    public static class CategoryRequest {
        @NotBlank(message = "分类名不能为空")
        private String name;
    }
}
