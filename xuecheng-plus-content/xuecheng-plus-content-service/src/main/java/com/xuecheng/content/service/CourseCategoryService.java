package com.xuecheng.content.service;

import com.xuecheng.content.model.dto.CourseCategoryTreeDto;

import java.util.List;

/**
 * 课程分类服务
 *
 * @author zhangyang
 * @date 2024/07/12
 */
public interface CourseCategoryService {
    /**
     * 查询树节点
     *
     * @return 列表<课程分类树>
     */
    List<CourseCategoryTreeDto> queryTreeNodes();
}
