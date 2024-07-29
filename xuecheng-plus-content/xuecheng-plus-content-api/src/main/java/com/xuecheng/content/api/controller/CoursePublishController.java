package com.xuecheng.content.api.controller;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSON;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.content.model.po.CourseTeacher;
import com.xuecheng.content.service.CoursePublishService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

/**
 * 课程发布控制层
 *
 * @author 张杨
 * @date 2024/07/20
 */
@Api("课程发布相关接口")
@Controller
@RequiredArgsConstructor
public class CoursePublishController {
    private final CoursePublishService coursePublishService;

    /**
     * 预览课程
     *
     * @param courseId 课程id
     * @return 模型和视图
     */
    @ApiOperation("预览课程")
    @GetMapping("/coursepreview/{courseId}")
    public ModelAndView preview(@PathVariable("courseId") Long courseId) {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject("model", coursePublishService.getCoursePreviewInfo(courseId));
        modelAndView.setViewName("course_template");
        return modelAndView;
    }

    /**
     * 提交审核
     *
     * @param courseId 课程id
     */
    @ApiOperation("提交审核")
    @ResponseBody
    @PostMapping("/courseaudit/commit/{courseId}")
    public void commitAudit(@PathVariable("courseId") Long courseId) {
        long companyId = 1232141425L;
        coursePublishService.commitAudit(companyId, courseId);
    }

    /**
     * 课程发布
     *
     * @param courseId 课程id
     */
    @ApiOperation("课程发布")
    @ResponseBody
    @PostMapping("/coursepublish/{courseId}")
    public void coursepublish(@PathVariable("courseId") Long courseId) {
        long companyId = 1232141425L;
        coursePublishService.publish(companyId, courseId);
    }

    /**
     * 查询课程发布信息
     *
     * @param courseId 课程id
     * @return <p>
     */
    @ApiOperation("查询课程发布信息")
    @ResponseBody
    @GetMapping("/r/coursepublish/{courseId}")
    public CoursePublish getCoursepublish(@PathVariable("courseId") Long courseId) {
        return coursePublishService.getCoursePublish(courseId);
    }

    /**
     * 获取课程发布信息
     *
     * @param courseId 课程ID
     * @return 课程预览DTO
     */
    @ApiOperation("获取课程发布信息")
    @ResponseBody
    @GetMapping("/course/whole/{courseId}")
    public CoursePreviewDto getCoursePublish(@PathVariable("courseId") Long courseId) {
        //查询发布的课程
        CoursePublish coursePublish = coursePublishService.getCoursePublish(courseId);
        if (coursePublish == null) {
            return CoursePreviewDto.builder().build();
        }
        //封装返回的数据
        return CoursePreviewDto.builder()
                .courseBase(BeanUtil.copyProperties(coursePublish, CourseBaseInfoDto.class))
                .teachplans(JSON.parseArray(coursePublish.getTeachplan(), TeachplanDto.class))
                .courseTeachers(JSON.parseArray(coursePublish.getTeachers(), CourseTeacher.class)).build();
    }
}
