package com.xuecheng.content.api.controller;

import com.xuecheng.content.model.dto.BindTeachplanMediaDto;
import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.service.TeachplanService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 课程计划接口
 *
 * @author zhangyang
 * @date 2024/07/14
 */
@Api("课程计划接口")
@RestController("/teachplan")
@RequiredArgsConstructor
public class TeachplanController {
    private final TeachplanService teachplanService;

    /**
     * 查询课程计划树
     *
     * @param courseId 课程id
     * @return <列表课程计划dto >
     */
    @ApiOperation("查询课程计划树")
    @GetMapping("/{courseId}/tree-nodes")
    public List<TeachplanDto> getTreeNodes(@PathVariable Long courseId) {
        return teachplanService.findTeachplanTree(courseId);
    }

    /**
     * 新增或修改课程计划
     *
     * @param saveTeachplanDto 新增课程计划dto
     */
    @ApiOperation("新增或修改课程计划")
    @PostMapping
    public void saveTeachplan(@RequestBody SaveTeachplanDto saveTeachplanDto) {
        teachplanService.saveTeachplan(saveTeachplanDto);
    }

    /**
     * 删除课程计划
     *
     * @param teachplanId 课程计划id
     */
    @ApiOperation("删除课程计划")
    @DeleteMapping("/{teachplanId}")
    public void deleteTeachplan(@PathVariable Long teachplanId) {
        teachplanService.deleteTeachplan(teachplanId);
    }

    /**
     * 移动课程计划
     *
     * @param moveType    移动类型
     * @param teachplanId 课程计划id
     */
    @PostMapping("/{moveType}/{teachplanId}")
    public void moveTeachplan(@PathVariable String moveType, @PathVariable Long teachplanId) {
        teachplanService.moveTeachplan(moveType, teachplanId);
    }

    /**
     * 绑定媒资文件
     *
     * @param bindTeachplanMediaDto 绑定课程计划媒体dto
     */
    @ApiOperation(value = "课程计划和媒资信息绑定")
    @PostMapping("/association/media")
    public void associationMedia(@RequestBody BindTeachplanMediaDto bindTeachplanMediaDto) {
        teachplanService.associationMedia(bindTeachplanMediaDto);
    }

    /**
     * 解除绑定媒资文件
     *
     * @param teachPlanId 教学计划id
     * @param mediaId     媒体id
     */
    @ApiOperation("解除绑定媒资文件")
    @DeleteMapping("/association/media/{teachPlanId}/{mediaId}")
    public void unbindMedia(@PathVariable Long teachPlanId, @PathVariable String mediaId) {
        teachplanService.unbindMedia(teachPlanId, mediaId);
    }

    /**
     * 是否可以预览视频
     *
     * @param teachPlanId 教学计划ID
     * @return 是否
     */
    @GetMapping("/{teachplanId}")
    public boolean isPreview(@PathVariable("teachplanId") Long teachPlanId) {
        return teachplanService.isPreview(teachPlanId);
    }
}

