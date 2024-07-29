package com.xuecheng.content.service;

import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.model.dto.AddCourseDto;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.EditCourseDto;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;

/**
 * 课程基础信息服务
 *
 * @author zhangyang
 * @date 2024/07/12
 */
public interface CourseBaseInfoService {

    /**
     * 查询课程列表
     *
     * @param pageParams           页面参数
     * @param queryCourseParamsDto 查询课程参数
     * @return 分页结果<课程基本信息>
     */
    PageResult<CourseBase> queryCourseBaseList(Long companyId, PageParams pageParams, QueryCourseParamsDto queryCourseParamsDto);


    /**
     * 新增课程
     *
     * @param companyId    公司标识
     * @param addCourseDto 添加课程d
     * @return 课程基础信息
     */
    CourseBaseInfoDto createCourseBase(Long companyId, AddCourseDto addCourseDto);


    /**
     * 获取课程基础信息
     *
     * @param courseId 课程id
     * @return 课程基础信息
     */
    CourseBaseInfoDto getCourseBaseInfo(Long courseId);


    /**
     * 更新课程基础
     *
     * @param companyId     公司标识
     * @param editCourseDto 修改课程dto
     * @return 课程基础信息
     */
    CourseBaseInfoDto updateCourseBase(Long companyId, EditCourseDto editCourseDto);

    /**
     * 删除课程
     *
     * @param companyId 公司标识
     * @param courseId  课程id
     */
    void deleteCourse(Long companyId, Long courseId);
}
