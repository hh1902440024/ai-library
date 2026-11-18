package com.library.mapper;

import com.library.dto.CategoryVO;
import com.library.entity.Category;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 分类表 Mapper
 */
public interface CategoryMapper {

    /** 分类列表（含图书数量） */
    List<CategoryVO> findAll();

    CategoryVO findVoById(Long id);

    int insert(Category category);

    int updateById(Category category);

    int deleteById(Long id);

    /** 该分类下图书数量（删除分类前校验） */
    int countBooks(@Param("categoryId") Long categoryId);
}
