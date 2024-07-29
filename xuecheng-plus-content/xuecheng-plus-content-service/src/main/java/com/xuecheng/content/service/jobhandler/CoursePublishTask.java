package com.xuecheng.content.service.jobhandler;

import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.feignclient.CourseIndex;
import com.xuecheng.content.feignclient.SearchServiceClient;
import com.xuecheng.content.mapper.CoursePublishMapper;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.content.service.CoursePublishService;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MessageProcessAbstract;
import com.xuecheng.messagesdk.service.MqMessageService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * 课程发布任务
 *
 * @author reslou
 * @date 2024/07/22
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CoursePublishTask extends MessageProcessAbstract {
    private final CoursePublishService coursePublishService;
    private final CoursePublishMapper coursePublishMapper;
    private final SearchServiceClient searchServiceClient;

    //任务调度入口
    @XxlJob("CoursePublishJobHandler")
    public void coursePublishJobHandler() throws Exception {
        // 分片参数
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        log.debug("shardIndex=" + shardIndex + ",shardTotal=" + shardTotal);
        //参数:分片序号、分片总数、消息类型、一次最多取到的任务数量、一次任务调度执行的超时时间
        process(shardIndex, shardTotal, "course_publish", 30, 60);
    }

    /**
     * 执行
     *
     * @param mqMessage mq消息
     * @return 是否成功
     */
    @Override
    public boolean execute(MqMessage mqMessage) {
        long courseId = Integer.parseInt(mqMessage.getBusinessKey1());
        //课程静态化
        generateCourseHtml(mqMessage, courseId);
        saveCourseIndex(mqMessage, courseId);
        saveCourseCache(mqMessage, courseId);
        return false;
    }

    /**
     * 生成课程HTML并上传到 MinIO
     *
     * @param mqMessage mq消息
     * @param courseId  课程id
     */
    private void generateCourseHtml(MqMessage mqMessage, long courseId) {
        log.debug("开始进行课程静态化,课程id:{}", courseId);
        //幂等性处理
        Long taskId = mqMessage.getId();
        MqMessageService mqMessageService = this.getMqMessageService();
        if (mqMessageService.getStageOne(taskId) > 0) {
            log.debug("课程静态化已处理直接返回，课程id:{}", courseId);
            return;
        }
        //生成静态化页面
        File file = coursePublishService.generateCourseHtml(courseId);
        //上传静态化页面
        if (file != null) {
            coursePublishService.uploadCourseHtml(courseId, file);
        }
        //第一阶段完成
        mqMessageService.completedStageOne(taskId);
    }

    /**
     * 保存课程索引到 ES
     *
     * @param mqMessage mq消息
     * @param courseId  课程id
     */
    private void saveCourseIndex(MqMessage mqMessage, long courseId) {
        log.debug("开始进行保存课程索引,课程id:{}", courseId);
        //幂等性处理
        Long taskId = mqMessage.getId();
        MqMessageService mqMessageService = this.getMqMessageService();
        if (mqMessageService.getStageTwo(taskId) > 0) {
            log.debug("保存课程索引已处理直接返回，课程id:{}", courseId);
            return;
        }
        CoursePublish coursePublish = coursePublishMapper.selectById(courseId);
        CourseIndex courseIndex = new CourseIndex();
        BeanUtils.copyProperties(coursePublish,courseIndex);
        Boolean add = searchServiceClient.add(courseIndex);
        if (!add){
            XueChengPlusException.cast("添加索引失败");
        }
        //第二阶段完成
        mqMessageService.completedStageTwo(taskId);
    }

    /**
     * 保存课程缓存到 Redis
     *
     * @param mqMessage mq消息
     * @param courseId  课程id
     */
    private void saveCourseCache(MqMessage mqMessage, long courseId) {
        log.debug("开始进行保存课程缓存,课程id:{}", courseId);
        //幂等性处理
        Long taskId = mqMessage.getId();
        MqMessageService mqMessageService = this.getMqMessageService();
        if (mqMessageService.getStageThree(taskId) > 0) {
            log.debug("保存课程缓存已处理直接返回，课程id:{}", courseId);
            return;
        }
        int i = 1 / 0;
        //第三阶段完成
        mqMessageService.completedStageThree(taskId);
    }
}
