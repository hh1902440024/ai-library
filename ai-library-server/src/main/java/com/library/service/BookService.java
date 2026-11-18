package com.library.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.library.common.ResultCode;
import com.library.dto.BookDTO;
import com.library.dto.BookQuery;
import com.library.dto.BookVO;
import com.library.dto.PageResult;
import com.library.entity.Book;
import com.library.exception.BizException;
import com.library.mapper.BookMapper;
import com.library.mapper.BorrowRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 图书服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookService {

    private final BookMapper bookMapper;
    private final BorrowRecordMapper borrowRecordMapper;

    /** 多条件分页查询（公开） */
    public PageResult<BookVO> findByQuery(BookQuery query) {
        PageHelper.startPage(query.getPage(), query.getSize());
        List<BookVO> list = bookMapper.findVoByQuery(query);
        long total = new PageInfo<>(list).getTotal();
        return PageResult.of(total, list);
    }

    /** 图书详情（公开） */
    public BookVO findDetail(Long id) {
        BookVO vo = bookMapper.findVoById(id);
        if (vo == null) {
            throw new BizException(ResultCode.NOT_FOUND, "图书不存在");
        }
        return vo;
    }

    /** 热门图书（按借阅量，公开） */
    public List<BookVO> findHot(int limit) {
        return bookMapper.findHotBooks(limit);
    }

    /** 同分类推荐（详情页，公开） */
    public List<BookVO> findSimilar(Long id, int limit) {
        BookVO book = bookMapper.findVoById(id);
        if (book == null) {
            throw new BizException(ResultCode.NOT_FOUND, "图书不存在");
        }
        return bookMapper.findSimilarBooks(id, book.getCategoryId(), limit);
    }

    /** 新增图书（管理员）：ISBN 查重，初始可借库存 = 总库存 */
    public void save(BookDTO dto) {
        if (bookMapper.findByIsbn(dto.getIsbn()) != null) {
            throw new BizException("ISBN 已存在");
        }
        Book book = new Book();
        copy(dto, book);
        bookMapper.insert(book);
        log.info("新增图书: id={}, title={}", book.getId(), book.getTitle());
    }

    /** 修改图书（管理员）：总库存变化时同步调整可借库存 */
    public void update(Long id, BookDTO dto) {
        Book old = bookMapper.findById(id);
        if (old == null) {
            throw new BizException(ResultCode.NOT_FOUND, "图书不存在");
        }
        Book byIsbn = bookMapper.findByIsbn(dto.getIsbn());
        if (byIsbn != null && !byIsbn.getId().equals(id)) {
            throw new BizException("ISBN 已存在");
        }
        int borrowed = old.getTotalStock() - old.getAvailableStock();
        if (dto.getTotalStock() < borrowed) {
            throw new BizException("总库存不能小于当前借出数量（" + borrowed + "）");
        }
        Book book = new Book();
        book.setId(id);
        copy(dto, book);
        bookMapper.updateById(book);

        // 可借库存随总库存差额同步
        int delta = dto.getTotalStock() - old.getTotalStock();
        if (delta != 0) {
            bookMapper.adjustStock(id, delta);
        }
        log.info("修改图书: id={}, title={}", id, dto.getTitle());
    }

    /** 删除图书（管理员）：有借出中的记录时禁止 */
    public void delete(Long id) {
        Book book = bookMapper.findById(id);
        if (book == null) {
            throw new BizException(ResultCode.NOT_FOUND, "图书不存在");
        }
        if (borrowRecordMapper.countActiveByBook(id) > 0) {
            throw new BizException("该书有借出中的记录，无法删除");
        }
        bookMapper.deleteById(id);
        log.info("删除图书: id={}, title={}", id, book.getTitle());
    }

    private void copy(BookDTO dto, Book book) {
        book.setIsbn(dto.getIsbn());
        book.setTitle(dto.getTitle());
        book.setAuthor(dto.getAuthor());
        book.setPublisher(dto.getPublisher());
        book.setCategoryId(dto.getCategoryId());
        book.setCover(dto.getCover());
        book.setDescription(dto.getDescription());
        book.setTags(dto.getTags());
        book.setTotalStock(dto.getTotalStock());
        book.setPublishDate(dto.getPublishDate());
    }
}
