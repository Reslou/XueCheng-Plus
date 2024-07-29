package com.xuecheng.content.api.controller;

import com.xuecheng.base.exception.ValidationGroups;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.api.util.SecurityUtil;
import com.xuecheng.content.model.dto.AddCourseDto;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.EditCourseDto;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.service.CourseBaseInfoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 课程基础信息控制层
 *
 * @author zhangyang
 * @date 2024/07/12
 */
@Api(value = "课程信息编辑接口", tags = "课程信息编辑接口")
@RestController
@RequiredArgsConstructor
public class CourseBaseInfoController {

    private final CourseBaseInfoService courseBaseInfoService;

    /**
     * 查询课程
     *
     * @param pageParams           页面参数
     * @param queryCourseParamsDto 查询课程参数dto
     * @return 分页结果<课程>
     */
    @ApiOperation("查询课程")
    @PreAuthorize("hasAnyAuthority('xc_teachmanager_course_list')")//设置了@PreAuthorize表示执行此方法需要授权
    @PostMapping("/course/list")
    public PageResult<CourseBase> list(
            PageParams pageParams, @RequestBody(required = false) QueryCourseParamsDto queryCourseParamsDto) {
        String companyId = SecurityUtil.getUser().getCompanyId();

        return courseBaseInfoService.queryCourseBaseList(Long.parseLong(companyId),pageParams, queryCourseParamsDto);
    }


    /**
     * 创建课程基础
     *
     * @param addCourseDto 添加课程dto
     * @return 课程信息
     */
    @ApiOperation("添加课程")
    @PostMapping("/course")
    public CourseBaseInfoDto createCourseBase(
            @RequestBody @Validated(ValidationGroups.Insert.class) AddCourseDto addCourseDto) {
        long companyId = 1232141425L;
        return courseBaseInfoService.createCourseBase(companyId, addCourseDto);
    }


    /**
     * 通过id获取课程信息
     *
     * @param courseId 课程id
     * @return 课程信息
     */
    @ApiOperation("根据课程id查询课程基础信息")
    @GetMapping("/course/{courseId}")
    public CourseBaseInfoDto getCourseBaseById(@PathVariable Long courseId) {
        //取出当前用户身份
        SecurityUtil.XcUser user = SecurityUtil.getUser();
        System.out.println("user = " + user);
        return courseBaseInfoService.getCourseBaseInfo(courseId);
    }

    /**
     * 修改课程基础信息
     *
     * @param editCourseDto 修改课程dto
     * @return 课程基础信息
     */
    @ApiOperation("修改课程基础信息")
    @PutMapping("/course")
    public CourseBaseInfoDto modifyCourseBase(@RequestBody @Validated EditCourseDto editCourseDto) {
        Long companyId = 1232141425L;
        return courseBaseInfoService.updateCourseBase(companyId, editCourseDto);
    }

    /**
     * 删除课程
     *
     * @param courseId 课程id
     */
    @ApiOperation("删除课程")
    @DeleteMapping("/course/{courseId}")
    public void deleteCourse(@PathVariable Long courseId) {
        Long companyId = 1232141425L;
        courseBaseInfoService.deleteCourse(companyId, courseId);
    }
}
