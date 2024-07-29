package com.xuecheng.content.service;

import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.model.po.CoursePublish;

import java.io.File;

/**
 * 发布课程业务层
 *
 * @author reslou
 * @date 2024/07/20
 */
public interface CoursePublishService {
    /**
     * 获取课程预览信息
     *
     * @param courseId 课程id
     * @return 课程预览
     */
    CoursePreviewDto getCoursePreviewInfo(Long courseId);

    /**
     * 提交审核
     *
     * @param companyId 公司标识
     * @param courseId  课程id
     */
    void commitAudit(Long companyId, Long courseId);

    /**
     * 发布课程
     *
     * @param companyId 公司标识
     * @param courseId  课程id
     */
    void publish(Long companyId, Long courseId);

    /**
     * @param courseId 课程id
     * @return File 静态化文件
     * @description 课程静态化
     * @author Mr.M
     * @date 2022/9/23 16:59
     */
    File generateCourseHtml(Long courseId);

    /**
     * @param file 静态化文件
     * @description 上传课程静态化页面
     * @author Mr.M
     * @date 2022/9/23 16:59
     */
    void uploadCourseHtml(Long courseId, File file);

    /**
     * 查询发布的课程
     *
     * @param courseId 课程id
     * @return <p>
     */
    CoursePublish getCoursePublish(Long courseId);
}
