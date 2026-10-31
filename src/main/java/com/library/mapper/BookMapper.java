package com.library.mapper;

import com.library.dto.BookQuery;
import com.library.dto.BookVO;
import com.library.entity.Book;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 图书表 Mapper
 */
public interface BookMapper {

    int insert(Book book);

    int updateById(Book book);

    int deleteById(@Param("id") Long id);

    Book findById(@Param("id") Long id);

    /** 详情（联表分类名） */
    BookVO findVoById(@Param("id") Long id);

    /** 多条件分页查询（分页由 PageHelper 拦截，联表分类名） */
    List<BookVO> findVoByQuery(BookQuery query);

    /** ISBN 查重 */
    Book findByIsbn(@Param("isbn") String isbn);

    /** AI 馆藏检索（简化 RAG）：title/author/tags/description 模糊匹配，按可借库存排序 */
    List<Book> searchForRag(@Param("keyword") String keyword,
                            @Param("categoryId") Long categoryId,
                            @Param("limit") int limit);

    /** 借书：乐观扣减库存，返回影响行数（0 表示库存不足） */
    int decreaseStock(@Param("bookId") Long bookId);

    /** 还书：恢复库存 */
    int increaseStock(@Param("bookId") Long bookId);

    /** 管理员调整总库存时同步可借库存差值 */
    int adjustStock(@Param("id") Long id, @Param("delta") int delta);

    /** 热门图书（按借阅量降序，无借阅的按 id 兜底） */
    List<BookVO> findHotBooks(@Param("limit") int limit);

    /** 同分类推荐（排除自身，可借优先） */
    List<BookVO> findSimilarBooks(@Param("id") Long id, @Param("categoryId") Long categoryId, @Param("limit") int limit);
}
