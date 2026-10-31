package com.library.service;

import com.library.common.ResultCode;
import com.library.dto.CategoryVO;
import com.library.entity.Category;
import com.library.exception.BizException;
import com.library.mapper.BookMapper;
import com.library.mapper.CategoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 分类服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryMapper categoryMapper;
    private final BookMapper bookMapper;

    /** 分类列表（含图书数量，公开） */
    public List<CategoryVO> findAll() {
        return categoryMapper.findAll();
    }

    /** 新增分类（管理员），分类名唯一 */
    public void save(String name) {
        Category category = new Category();
        category.setName(name);
        categoryMapper.insert(category);
    }

    /** 修改分类（管理员） */
    public void update(Long id, String name) {
        if (categoryMapper.findVoById(id) == null) {
            throw new BizException(ResultCode.NOT_FOUND, "分类不存在");
        }
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        categoryMapper.updateById(category);
    }

    /** 删除分类（管理员）：分类下有图书时禁止 */
    public void delete(Long id) {
        if (categoryMapper.findVoById(id) == null) {
            throw new BizException(ResultCode.NOT_FOUND, "分类不存在");
        }
        if (categoryMapper.countBooks(id) > 0) {
            throw new BizException("该分类下仍有图书，无法删除");
        }
        categoryMapper.deleteById(id);
        log.info("删除分类: id={}", id);
    }
}
