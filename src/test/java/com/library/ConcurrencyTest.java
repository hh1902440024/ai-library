package com.library;

import com.library.dto.RegisterDTO;
import com.library.service.BorrowService;
import com.library.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 设计方案 §12.1 关键测试用例（直连本地 ai-library 数据库，自动构造并清理测试数据）：
 * TC02 并发注册同名用户 → 仅一人成功（唯一索引兜底）
 * TC05 库存 1 时并发 2 人借同一本书 → 1 成功 1 失败（乐观扣减防超卖）
 * TC07 逾期标记 → 超期未还记录状态变 2
 */
@SpringBootTest
class ConcurrencyTest {

    /** admin（本地库初始化用户） */
    private static final long ADMIN_USER_ID = 1L;

    @Autowired
    private UserService userService;

    @Autowired
    private BorrowService borrowService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** TC02：并发双线程注册同名用户，仅一人成功 */
    @Test
    void tc02_concurrentRegisterSameUsername_onlyOneSucceeds() throws Exception {
        String username = "tc02_" + System.currentTimeMillis();
        AtomicInteger success = new AtomicInteger();
        AtomicInteger phoneSeed = new AtomicInteger(10000000);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);

        for (int i = 0; i < 2; i++) {
            pool.execute(() -> {
                try {
                    start.await();
                    RegisterDTO dto = new RegisterDTO();
                    dto.setUsername(username);
                    dto.setPhone("139" + phoneSeed.incrementAndGet());
                    dto.setPassword("123456");
                    dto.setConfirmPassword("123456");
                    userService.register(dto);
                    success.incrementAndGet();
                } catch (Exception ignored) {
                    // 另一方预期失败（用户名已存在 / 唯一索引冲突）
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        done.await();
        pool.shutdown();

        assertEquals(1, success.get(), "并发注册同名用户应恰好一人成功");
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user WHERE username = ?", Integer.class, username);
        assertEquals(1, count, "数据库中该用户名仅一条记录");

        jdbcTemplate.update("DELETE FROM user WHERE username = ?", username);
    }

    /** TC05：库存 1 时并发 2 人借同一本书，1 成功 1 失败，无超卖 */
    @Test
    void tc05_concurrentBorrowLastCopy_noOversell() throws Exception {
        String isbn = "TC05-" + System.currentTimeMillis();
        jdbcTemplate.update(
                "INSERT INTO book (isbn, title, author, category_id, total_stock, available_stock) VALUES (?, ?, ?, 1, 1, 1)",
                isbn, "并发测试图书", "测试作者");
        Long bookId = jdbcTemplate.queryForObject("SELECT id FROM book WHERE isbn = ?", Long.class, isbn);

        AtomicInteger success = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);

        for (int i = 0; i < 2; i++) {
            pool.execute(() -> {
                try {
                    start.await();
                    borrowService.borrow(ADMIN_USER_ID, bookId);
                    success.incrementAndGet();
                } catch (Exception ignored) {
                    // 另一方预期失败（库存不足）
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        done.await();
        pool.shutdown();

        assertEquals(1, success.get(), "库存 1 并发 2 借应恰好一人成功");
        Integer available = jdbcTemplate.queryForObject(
                "SELECT available_stock FROM book WHERE id = ?", Integer.class, bookId);
        assertEquals(0, available, "可借库存应为 0（无超卖）");
        Integer records = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM borrow_record WHERE book_id = ?", Integer.class, bookId);
        assertEquals(1, records, "借阅记录应仅一条");

        jdbcTemplate.update("DELETE FROM borrow_record WHERE book_id = ?", bookId);
        jdbcTemplate.update("DELETE FROM book WHERE id = ?", bookId);
    }

    /** TC07：逾期后标记任务执行，记录状态变 2 */
    @Test
    void tc07_markOverdueTask_marksExpiredRecord() {
        // 造一条昨天到期、仍借出中的记录（挂到已存在的图书上）
        jdbcTemplate.update(
                "INSERT INTO borrow_record (user_id, book_id, borrow_time, due_time, status) "
                        + "SELECT ?, id, DATE_SUB(NOW(), INTERVAL 31 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), 0 "
                        + "FROM book LIMIT 1", ADMIN_USER_ID);
        Long recordId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM borrow_record", Long.class);

        try {
            borrowService.markOverdueNow();
            Integer status = jdbcTemplate.queryForObject(
                    "SELECT status FROM borrow_record WHERE id = ?", Integer.class, recordId);
            assertEquals(2, status, "超期未还记录应被标记为逾期(2)");
        } finally {
            jdbcTemplate.update("DELETE FROM borrow_record WHERE id = ?", recordId);
        }
    }
}
