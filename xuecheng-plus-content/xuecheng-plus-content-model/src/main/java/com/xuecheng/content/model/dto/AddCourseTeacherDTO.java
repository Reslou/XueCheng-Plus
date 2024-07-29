package com.xuecheng.content.model.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 添加课程教师
 *
 * @author zhangyang
 * @date 2024/07/15
 */
@Data
@ApiModel("添加课程教师")
public class AddCourseTeacherDTO {

    @ApiModelProperty("课程ID")
    private Long courseId;

    @ApiModelProperty("教师名称")
    private String teacherName;

    @ApiModelProperty("教师职位")
    private String position;

    @ApiModelProperty("教师简介")
    private String introduction;
}
