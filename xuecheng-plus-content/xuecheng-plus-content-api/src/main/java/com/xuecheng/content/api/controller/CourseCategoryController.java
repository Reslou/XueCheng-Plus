package com.xuecheng.content.api.controller;

import com.xuecheng.content.model.dto.CourseCategoryTreeDto;
import com.xuecheng.content.service.CourseCategoryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 课程类别控制器
 *
 * @author zhangyang
 * @date 2024/07/12
 */
@RestController
@RequiredArgsConstructor
@Api("课程相关")
public class CourseCategoryController {
    private final CourseCategoryService service;

    /**
     * 查询课程分类
     *
     * @return 列表<课程分类树>
     */
    @ApiOperation("查询课程分类")
    @GetMapping("course-category/tree-nodes")
    public List<CourseCategoryTreeDto> queryTreeNodes() {
        return service.queryTreeNodes();
    }

}
