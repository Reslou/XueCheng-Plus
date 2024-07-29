package com.xuecheng.content.service;

import com.xuecheng.content.model.dto.AddCourseTeacherDTO;
import com.xuecheng.content.model.po.CourseTeacher;

import java.util.List;

/**
 * 课程老师业务层
 *
 * @author zhangyang
 * @date 2024/07/15
 */
public interface CourseTeacherService {

    /**
     * 列出课程教师
     *
     * @param courseId 课程id
     * @return 列表<课程教师>
     */
    List<CourseTeacher> listCourseTeacher(Long courseId);

    /**
     * 添加教师
     *
     * @param teacherDTO 教师dto
     * @return <p>
     */
    CourseTeacher addTeacher(AddCourseTeacherDTO teacherDTO, Long companyId);

    /**
     * 修改教师
     *
     * @param courseTeacher 课程教师
     * @return <p>
     */
    CourseTeacher editTeacher(CourseTeacher courseTeacher);

    /**
     * 删除教师
     *
     * @param courseId  课程id
     * @param teacherId 教师id
     */
    void deleteTeacher(Long courseId, Long teacherId, Long companyId);
}
