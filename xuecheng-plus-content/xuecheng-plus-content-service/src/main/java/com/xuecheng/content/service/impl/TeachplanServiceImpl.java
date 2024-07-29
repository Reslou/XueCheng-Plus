package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.mapper.TeachplanMapper;
import com.xuecheng.content.mapper.TeachplanMediaMapper;
import com.xuecheng.content.model.dto.BindTeachplanMediaDto;
import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.model.po.TeachplanMedia;
import com.xuecheng.content.service.TeachplanService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 课程计划业务层实现类
 *
 * @author zhangyang
 * @date 2024/07/14
 */
@Service
@RequiredArgsConstructor
public class TeachplanServiceImpl implements TeachplanService {

    private final TeachplanMapper teachplanMapper;
    private final TeachplanMediaMapper teachplanMediaMapper;

    /**
     * 查询课程计划树
     *
     * @param courseId 课程id
     * @return <列表课程计划dto >
     */
    @Override
    public List<TeachplanDto> findTeachplanTree(Long courseId) {
        return teachplanMapper.selectTreeNodes(courseId);
    }

    /**
     * 新增或修改课程计划
     *
     * @param saveTeachplanDto 保存课程计划dto
     */
    @Transactional
    @Override
    public void saveTeachplan(SaveTeachplanDto saveTeachplanDto) {
        Long id = saveTeachplanDto.getId();
        //根据课程id有无,修改或新增
        if (id != null) {
            Teachplan teachplan = teachplanMapper.selectById(id);
            BeanUtils.copyProperties(saveTeachplanDto, teachplan);
            teachplanMapper.updateById(teachplan);
        } else {
            //查询同级课程计划数量
            LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<Teachplan>()
                    .eq(Teachplan::getCourseId, saveTeachplanDto.getCourseId())
                    .eq(Teachplan::getParentid, saveTeachplanDto.getParentid());
            Integer count = teachplanMapper.selectCount(queryWrapper);
            Teachplan teachplan = new Teachplan();
            teachplan.setOrderby(count + 1);
            BeanUtils.copyProperties(saveTeachplanDto, teachplan);
            teachplanMapper.insert(teachplan);
        }
    }

    /**
     * 删除课程计划
     *
     * @param teachplanId 课程计划id
     */
    @Transactional
    @Override
    public void deleteTeachplan(Long teachplanId) {
        Teachplan teachplan = teachplanMapper.selectById(teachplanId);
        Integer grade = teachplan.getGrade();
        if (grade == 1) {
            //一级课程计划
            Integer count = teachplanMapper.selectCount(
                    new LambdaQueryWrapper<Teachplan>()
                            .eq(Teachplan::getParentid, teachplanId));
            if (count != 0) {
                throw new XueChengPlusException(
                        "\"errCode\":\"120409\",\"errMessage\":\"课程计划信息还有子级信息，无法操作\"");
            }
        }
        if (grade == 2) {
            //二级课程计划
            List<TeachplanMedia> teachplanMedia = teachplanMediaMapper.selectList(
                    new LambdaQueryWrapper<TeachplanMedia>()
                            .eq(TeachplanMedia::getTeachplanId, teachplanId));
            //删除课程计划相关媒资
            if (!teachplanMedia.isEmpty()) {
                teachplanMediaMapper.deleteBatchIds(teachplanMedia);
            }
        }
        teachplanMapper.deleteById(teachplanId);
    }

    /**
     * 移动课程计划
     *
     * @param moveType    移动类型
     * @param teachplanId 课程计划id
     */
    @Transactional
    @Override
    public void moveTeachplan(String moveType, Long teachplanId) {
        Teachplan teachplan = teachplanMapper.selectById(teachplanId);
        Integer orderby = teachplan.getOrderby();
        if (moveType.equals("movedown")) {
            int downOrderby = orderby + 1;
            exchangeOrderby(teachplan, orderby, downOrderby);
        }
        if (moveType.equals("moveup")) {
            int upOrderby = orderby - 1;
            exchangeOrderby(teachplan, orderby, upOrderby);
        }
    }

    /**
     * 链接媒体
     *
     * @param bindTeachplanMediaDto 绑定课程计划媒体dto
     */
    @Transactional
    @Override
    public void associationMedia(BindTeachplanMediaDto bindTeachplanMediaDto) {
        //校验教学计划
        Long teachplanId = bindTeachplanMediaDto.getTeachplanId();
        Teachplan teachplan = teachplanMapper.selectById(teachplanId);
        if (teachplan == null) {
            XueChengPlusException.cast("教学计划不存在");
        }
        if (teachplan.getGrade() != 2) {
            XueChengPlusException.cast("只有第二级的教学计划才能绑定媒资文件");
        }
        //删除旧的媒资文件
        teachplanMediaMapper.delete(new LambdaQueryWrapper<TeachplanMedia>()
                .eq(TeachplanMedia::getTeachplanId, teachplanId));
        //添加新的媒资文件
        TeachplanMedia teachplanMedia = new TeachplanMedia();
        BeanUtils.copyProperties(bindTeachplanMediaDto, teachplanMedia);
        teachplanMedia.setCourseId(teachplan.getCourseId());
        teachplanMedia.setMediaFilename(bindTeachplanMediaDto.getFileName());
        teachplanMedia.setCreateDate(LocalDateTime.now());
        teachplanMediaMapper.insert(teachplanMedia);
    }

    /**
     * 解除绑定媒资文件
     *
     * @param teachPlanId 教学计划id
     * @param mediaId     媒体id
     */
    @Override
    public void unbindMedia(Long teachPlanId, String mediaId) {
        teachplanMediaMapper.delete(new LambdaQueryWrapper<TeachplanMedia>()
                .eq(TeachplanMedia::getTeachplanId, teachPlanId));
    }

    /**
     * 是否可以预览视频
     *
     * @param teachPlanId 教学计划ID
     * @return 是否
     */
    @Override
    public boolean isPreview(Long teachPlanId) {
        Teachplan teachplan = teachplanMapper.selectById(teachPlanId);
        String isPreview = teachplan.getIsPreview();
        if (isPreview == null) {
            return false;
        }
        return Integer.parseInt(isPreview) == 1;
    }

    /**
     * 交换排序字段
     *
     * @param teachplan       课程计划
     * @param orderby         排序字段
     * @param exchangeOrderby 要交换的排序字段
     */
    private void exchangeOrderby(Teachplan teachplan, Integer orderby, int exchangeOrderby) {
        Teachplan exchangeTeachplan = teachplanMapper.selectOne(
                new LambdaQueryWrapper<Teachplan>()
                        .eq(Teachplan::getParentid, teachplan.getParentid())
                        .eq(Teachplan::getOrderby, exchangeOrderby));
        if (exchangeTeachplan == null) {
            throw new XueChengPlusException("最上(下)面的课程计划不允许向上(下)移动");
        }
        teachplan.setOrderby(exchangeOrderby);
        teachplanMapper.updateById(teachplan);
        exchangeTeachplan.setOrderby(orderby);
        teachplanMapper.updateById(exchangeTeachplan);
    }
}
