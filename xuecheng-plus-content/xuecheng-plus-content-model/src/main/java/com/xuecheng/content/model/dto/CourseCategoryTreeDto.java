package com.xuecheng.content.model.dto;

import com.xuecheng.content.model.po.CourseCategory;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

/**
 * 课程分类树
 *
 * @author zhangyang
 * @date 2024/07/12
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("课程分类树")
public class CourseCategoryTreeDto extends CourseCategory implements Serializable {

    @ApiModelProperty("子节点")
    List<CourseCategoryTreeDto> childrenTreeNodes;
}
