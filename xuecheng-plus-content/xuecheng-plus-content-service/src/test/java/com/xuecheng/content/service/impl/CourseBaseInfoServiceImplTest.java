package com.xuecheng.content.service.impl;

import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.service.CourseBaseInfoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CourseBaseInfoServiceImplTest {
    @Autowired
    private CourseBaseInfoService courseBaseInfoService;

    @Test
    void queryCourseBaseList() {
        //获取查询参数
        PageParams pageParams = new PageParams();
        pageParams.setPageNo(1L);//页码
        pageParams.setPageSize(3L);//每页记录数
        QueryCourseParamsDto dto = new QueryCourseParamsDto();
        dto.setCourseName("java");
        dto.setAuditStatus("202004");
        dto.setPublishStatus("203001");

        PageResult<CourseBase> pageResult = courseBaseInfoService.queryCourseBaseList(null,pageParams, dto);
        System.out.println("pageResult = " + pageResult);
    }
}