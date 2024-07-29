package com.xuecheng.content.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;
import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CourseBaseMapperTest {
    @Autowired
    private CourseBaseMapper courseBaseMapper;

    @Test
    void testCourseBaseMapper() {
        CourseBase courseBase = courseBaseMapper.selectById(18);
        Assertions.assertNotNull(courseBase);
        //获取查询参数
        PageParams pageParams = new PageParams();
        pageParams.setPageNo(1L);//页码
        pageParams.setPageSize(3L);//每页记录数
        Long pageSize = pageParams.getPageSize();
        Long pageNo = pageParams.getPageNo();
        QueryCourseParamsDto dto = new QueryCourseParamsDto();
        dto.setCourseName("java");
        dto.setAuditStatus("202004");
        dto.setPublishStatus("203001");
        String courseName = dto.getCourseName();
        String auditStatus = dto.getAuditStatus();
        String publishStatus = dto.getPublishStatus();
        //构建查询条件
        LambdaQueryWrapper<CourseBase> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper
                .like(StringUtils.isNotEmpty(courseName),CourseBase::getName,courseName)
                .eq(StringUtils.isNotEmpty(auditStatus),CourseBase::getAuditStatus,auditStatus)
                .eq(StringUtils.isNotEmpty(publishStatus),CourseBase::getStatus,publishStatus);
        //构建分页条件
        Page<CourseBase> page = new Page<>(pageNo,pageSize);
        //分页查询
        page= courseBaseMapper.selectPage(page, queryWrapper);
        PageResult<CourseBase> pageResult = new PageResult<>(page.getRecords(), page.getTotal(), pageNo, pageSize);
        System.out.println("pageResult = " + pageResult);
    }

}