package com.xuecheng.content.service;

import com.xuecheng.content.model.dto.BindTeachplanMediaDto;
import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;

import java.util.List;

/**
 * 课程计划业务层
 *
 * @author zhangyang
 * @date 2024/07/14
 */
public interface TeachplanService {

    /**
     * 查询课程计划树
     *
     * @param courseId 课程id
     * @return <列表课程计划dto >
     */
    List<TeachplanDto> findTeachplanTree(Long courseId);

    /**
     * 新增或修改课程计划
     *
     * @param saveTeachplanDto 保存课程计划dto
     */
    void saveTeachplan(SaveTeachplanDto saveTeachplanDto);

    /**
     * 删除课程计划
     *
     * @param teachplanId 课程计划id
     */
    void deleteTeachplan(Long teachplanId);

    /**
     * 移动课程计划
     *
     * @param moveType    移动类型
     * @param teachplanId 课程计划id
     */
    void moveTeachplan(String moveType, Long teachplanId);


    /**
     * 链接媒体
     *
     * @param bindTeachplanMediaDto 绑定课程计划媒体dto
     */
    void associationMedia(BindTeachplanMediaDto bindTeachplanMediaDto);

    /**
     * 解除绑定媒资文件
     *
     * @param teachPlanId 教学计划id
     * @param mediaId     媒体id
     */
    void unbindMedia(Long teachPlanId, String mediaId);

    /**
     * 是否可以预览视频
     *
     * @param teachPlanId 教学计划ID
     * @return 是否
     */
    boolean isPreview(Long teachPlanId);
}
