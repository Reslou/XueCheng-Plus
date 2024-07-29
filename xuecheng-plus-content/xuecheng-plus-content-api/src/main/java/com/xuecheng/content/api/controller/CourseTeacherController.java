package com.xuecheng.content.api.controller;

import com.xuecheng.content.model.dto.AddCourseTeacherDTO;
import com.xuecheng.content.model.po.CourseTeacher;
import com.xuecheng.content.service.CourseTeacherService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 课程老师控制层
 *
 * @author zhangyang
 * @date 2024/07/15
 */
@RestController
@Api("课程老师控制层")
@RequiredArgsConstructor
public class CourseTeacherController {

    private final CourseTeacherService service;

    /**
     * 查询课程教师
     *
     * @param courseId 课程id
     * @return 列表<课程教师>
     */
    @GetMapping("/courseTeacher/list/{courseId}")
    @ApiOperation("查询课程教师")
    public List<CourseTeacher> listCourseTeacher(@PathVariable Long courseId) {
        return service.listCourseTeacher(courseId);
    }


    /**
     * 添加教师
     *
     * @param teacherDTO 教师dto
     * @return <p>
     */
    @ApiOperation("添加教师")
    @PostMapping("/courseTeacher")
    public CourseTeacher addTeacher(@RequestBody AddCourseTeacherDTO teacherDTO) {
        Long companyId = 1232141425L;
        return service.addTeacher(teacherDTO, companyId);
    }


    /**
     * 修改教师
     *
     * @param courseTeacher 课程教师
     * @return <p>
     */
    @PutMapping("/courseTeacher")
    public CourseTeacher editTeacher(@RequestBody CourseTeacher courseTeacher) {
        return service.editTeacher(courseTeacher);
    }

    @DeleteMapping("/courseTeacher/course/{courseId}/{teacherId}")
    public void deleteTeacher(@PathVariable Long courseId, @PathVariable Long teacherId) {
        Long companyId = 1232141425L;
        service.deleteTeacher(courseId, teacherId, companyId);
    }
}
