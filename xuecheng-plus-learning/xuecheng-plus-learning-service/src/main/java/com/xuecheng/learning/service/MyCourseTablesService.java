package com.xuecheng.learning.service;

import com.xuecheng.base.model.PageResult;
import com.xuecheng.learning.model.dto.MyCourseTableParams;
import com.xuecheng.learning.model.dto.XcChooseCourseDto;
import com.xuecheng.learning.model.dto.XcCourseTablesDto;
import com.xuecheng.learning.model.po.XcCourseTables;

/**
 * @author Mr.M
 * @version 1.0
 * @description 我的课程表service接口
 * @date 2022/10/2 16:07
 */
public interface MyCourseTablesService {

    /**
     * @param userId   用户id
     * @param courseId 课程id
     * @return com.xuecheng.learning.model.dto.XcChooseCourseDto
     * @description 添加选课
     * @author Mr.M
     * @date 2022/10/24 17:33
     */
    XcChooseCourseDto addChooseCourse(String userId, Long courseId);

    /**
     * 获取学习状态
     *
     * @param userId   用户id
     * @param courseId 课程id
     * @return XcCourseTablesDto 学习资格状态
     */
    XcCourseTablesDto getLearningStatus(String userId, Long courseId);


    /**
     * 保存选择课程成功
     *
     * @param chooseCourseId 选择课程编号
     * @return 保存是否成功
     */
    boolean saveChooseCourseSuccess(String chooseCourseId);

    /**
     * 我的课程表
     *
     * @param params 我的课程表参数
     * @return 分页结果<课程表>
     */
    PageResult<XcCourseTables> myCourseTables(MyCourseTableParams params);
}