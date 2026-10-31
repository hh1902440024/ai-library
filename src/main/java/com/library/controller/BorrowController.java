package com.library.controller;

import com.library.common.Result;
import com.library.dto.BorrowQuery;
import com.library.dto.BorrowVO;
import com.library.dto.PageResult;
import com.library.interceptor.LoginUser;
import com.library.interceptor.RequireAdmin;
import com.library.interceptor.UserContext;
import com.library.service.BorrowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 借阅接口
 */
@Tag(name = "借阅")
@RestController
@RequestMapping("/api/borrows")
@RequiredArgsConstructor
public class BorrowController {

    private final BorrowService borrowService;

    @Operation(summary = "借书（默认借期 30 天）")
    @PostMapping
    public Result<Void> borrow(@RequestBody Map<String, Long> body) {
        Long bookId = body.get("bookId");
        if (bookId == null) {
            return Result.fail("bookId 不能为空");
        }
        borrowService.borrow(UserContext.getUserId(), bookId);
        return Result.ok("借阅成功", null);
    }

    @Operation(summary = "还书（本人或管理员）")
    @PutMapping("/{id}/return")
    public Result<Void> returnBook(@PathVariable Long id) {
        LoginUser user = UserContext.get();
        borrowService.returnBook(user.userId(), id, user.role() == 1);
        return Result.ok("归还成功", null);
    }

    @Operation(summary = "我的借阅（分页+状态筛选）")
    @GetMapping("/my")
    public Result<PageResult<BorrowVO>> my(BorrowQuery query) {
        return Result.ok(borrowService.myBorrows(UserContext.getUserId(), query));
    }

    @RequireAdmin
    @Operation(summary = "全部借阅记录（多条件）")
    @GetMapping
    public Result<PageResult<BorrowVO>> list(BorrowQuery query) {
        return Result.ok(borrowService.adminList(query));
    }

    @RequireAdmin
    @Operation(summary = "立即执行逾期标记（定时任务的手动触发入口）")
    @PostMapping("/mark-overdue")
    public Result<Integer> markOverdue() {
        int rows = borrowService.markOverdueNow();
        return Result.ok("已标记 " + rows + " 条逾期记录", rows);
    }

    @RequireAdmin
    @Operation(summary = "导出借阅记录 Excel")
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(BorrowQuery query) {
        byte[] data = borrowService.exportExcel(query);
        String fileName = URLEncoder.encode("借阅记录.xlsx", StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fileName)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }
}
