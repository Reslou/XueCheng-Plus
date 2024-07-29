package com.xuecheng.content.model.dto;

import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.model.po.TeachplanMedia;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.ToString;

import java.util.List;

/**
 * 课程计划树dto
 *
 * @author zhangyang
 * @date 2024/07/14
 */
@Data
@ToString
@ApiModel(description = "课程计划树dto")
public class TeachplanDto extends Teachplan {

    @ApiModelProperty("关联的媒资")
    TeachplanMedia teachplanMedia;

    @ApiModelProperty("子节点")
    List<TeachplanDto> teachPlanTreeNodes;
}
