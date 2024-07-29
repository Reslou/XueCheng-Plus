package com.xuecheng.learning.service.impl;

import com.xuecheng.base.model.RestResponse;
import com.xuecheng.base.utils.StringUtil;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.learning.feignclient.ContentServiceClient;
import com.xuecheng.learning.feignclient.MediaServiceClient;
import com.xuecheng.learning.model.dto.XcCourseTablesDto;
import com.xuecheng.learning.service.LearningService;
import com.xuecheng.learning.service.MyCourseTablesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 学习业务层实现类
 *
 * @author 张杨
 * @date 2024/07/28
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class LearningServiceImpl implements LearningService {
    private final ContentServiceClient contentServiceClient;
    private final MediaServiceClient mediaServiceClient;
    private final MyCourseTablesService myCourseTablesService;

    /**
     * 获取视频
     *
     * @param userId      用户ID
     * @param courseId    课程ID
     * @param teachPlanId 教计划ID
     * @param mediaId     媒体ID
     * @return Rest响应<string>
     */
    @Override
    public RestResponse<String> getVideo(String userId, Long courseId, Long teachPlanId, String mediaId) {
        //查询发布的课程
        CoursePublish coursepublish = contentServiceClient.getCoursepublish(courseId);
        if (coursepublish == null) {
            return RestResponse.validfail("课程不存在");
        }
        //课程可以试学
        if (contentServiceClient.isPreview(teachPlanId)) {
            return mediaServiceClient.getPlayUrlByMediaId(mediaId);
        }
        //用户已登录
        if (StringUtil.isNotEmpty(userId)) {
            XcCourseTablesDto learningStatus = myCourseTablesService.getLearningStatus(userId, courseId);
            switch (learningStatus.getLearnStatus()) {
                case "702001"://正常学习
                    return mediaServiceClient.getPlayUrlByMediaId(mediaId);
                case "702002":
                    return RestResponse.validfail("无法观看，由于没有选课或选课后没有支付");
                case "702003":
                    return RestResponse.validfail("您的选课已过期需要申请续期或重新支付");
            }
        }
        //免费课程
        if (coursepublish.getCharge().equals("201000")) {
            return mediaServiceClient.getPlayUrlByMediaId(mediaId);
        }
        return RestResponse.validfail("请购买课程后继续学习");
    }
}
