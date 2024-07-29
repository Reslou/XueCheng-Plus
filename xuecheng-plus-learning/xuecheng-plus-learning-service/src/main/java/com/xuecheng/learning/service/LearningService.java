package com.xuecheng.learning.service;

import com.xuecheng.base.model.RestResponse;

/**
 * 学习业务层
 *
 * @author 张杨
 * @date 2024/07/28
 */
public interface LearningService {

    /**
     * 获取视频
     *
     * @param userId      用户ID
     * @param courseId    课程ID
     * @param teachPlanId 教计划ID
     * @param mediaId     媒体ID
     * @return Rest响应<string>
     */
    RestResponse<String> getVideo(String userId, Long courseId, Long teachPlanId, String mediaId);
}
