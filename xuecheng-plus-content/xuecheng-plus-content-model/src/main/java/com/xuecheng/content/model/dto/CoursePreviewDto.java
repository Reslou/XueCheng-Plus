package com.xuecheng.content.model.dto;

import com.xuecheng.content.model.po.CourseTeacher;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 课程预览DTO
 *
 * @author reslou
 * @date 2024/07/20
 */
@Data
@Builder
public class CoursePreviewDto {
    //课程基本信息,课程营销信息
    private CourseBaseInfoDto courseBase;
    //课程计划信息
    private List<TeachplanDto> teachplans;
    //师资信息
    private List<CourseTeacher> courseTeachers;
}
