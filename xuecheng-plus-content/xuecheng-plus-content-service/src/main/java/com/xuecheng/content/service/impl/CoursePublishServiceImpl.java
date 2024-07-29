package com.xuecheng.content.service.impl;

import com.alibaba.fastjson.JSON;
import com.xuecheng.base.exception.CommonError;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.utils.StringUtil;
import com.xuecheng.content.config.MultipartSupportConfig;
import com.xuecheng.content.feignclient.MediaServiceClient;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.mapper.CourseMarketMapper;
import com.xuecheng.content.mapper.CoursePublishMapper;
import com.xuecheng.content.mapper.CoursePublishPreMapper;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.*;
import com.xuecheng.content.service.CourseBaseInfoService;
import com.xuecheng.content.service.CoursePublishService;
import com.xuecheng.content.service.CourseTeacherService;
import com.xuecheng.content.service.TeachplanService;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MqMessageService;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

/**
 * 发布课程业务层实现类
 *
 * @author reslou
 * @date 2024/07/20
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CoursePublishServiceImpl implements CoursePublishService {

    private final CourseBaseInfoService courseBaseInfoService;
    private final CourseBaseMapper courseBaseMapper;
    private final CourseMarketMapper courseMarketMapper;
    private final TeachplanService teachplanService;
    private final CourseTeacherService courseTeacherService;
    private final CoursePublishPreMapper coursePublishPreMapper;
    private final CoursePublishMapper coursePublishMapper;
    private final MqMessageService mqMessageService;
    private final MediaServiceClient mediaServiceClient;

    /**
     * 获取课程预览信息
     *
     * @param courseId 课程id
     * @return 课程预览
     */
    @Override
    public CoursePreviewDto getCoursePreviewInfo(Long courseId) {
        CourseBaseInfoDto courseBaseInfo = courseBaseInfoService.getCourseBaseInfo(courseId);
        List<TeachplanDto> teachplanTree = teachplanService.findTeachplanTree(courseId);
        List<CourseTeacher> courseTeacherList = courseTeacherService.listCourseTeacher(courseId);
        return CoursePreviewDto.builder()
                .courseBase(courseBaseInfo)
                .teachplans(teachplanTree)
                .courseTeachers(courseTeacherList).build();
    }

    /**
     * 提交审核
     *
     * @param companyId 公司标识
     * @param courseId  课程id
     */
    @Transactional
    @Override
    public void commitAudit(Long companyId, Long courseId) {
        //校验课程信息
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        String Submitted = "202003";//已提交
        if (courseBase.getAuditStatus().equals(Submitted)) {
            XueChengPlusException.cast("当前为等待审核状态，审核完成可以再次提交。");
        }
        if (!courseBase.getCompanyId().equals(companyId)) {
            XueChengPlusException.cast("不允许提交其它机构的课程。");
        }
        if (StringUtil.isEmpty(courseBase.getPic())) {
            XueChengPlusException.cast("提交失败，请上传课程图片");
        }

        //添加课程预发布记录
        CoursePublishPre coursePublishPre = new CoursePublishPre();
        CourseBaseInfoDto courseBaseInfo = courseBaseInfoService.getCourseBaseInfo(courseId);
        BeanUtils.copyProperties(courseBaseInfo, coursePublishPre);
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);
        coursePublishPre.setMarket(JSON.toJSONString(courseMarket));
        List<CourseTeacher> courseTeacherList = courseTeacherService.listCourseTeacher(courseId);
        coursePublishPre.setTeachers(JSON.toJSONString(courseTeacherList));
        List<TeachplanDto> teachplanTree = teachplanService.findTeachplanTree(courseId);
        if (teachplanTree.isEmpty()) {
            XueChengPlusException.cast("提交失败，还没有添加课程计划");
        }
        coursePublishPre.setTeachplan(JSON.toJSONString(teachplanTree));
        coursePublishPre.setStatus(Submitted);
        coursePublishPre.setCreateDate(LocalDateTime.now());
        CoursePublishPre coursePublishPreUpdate = coursePublishPreMapper.selectById(courseId);
        if (coursePublishPreUpdate == null) {
            coursePublishPreMapper.insert(coursePublishPre);
        } else {
            coursePublishPreMapper.updateById(coursePublishPre);
        }

        //更新课程基本表的审核状态
        courseBase.setAuditStatus(Submitted);
        courseBaseMapper.updateById(courseBase);
    }

    /**
     * 发布课程
     *
     * @param companyId 公司标识
     * @param courseId  课程id
     */
    @Override
    public void publish(Long companyId, Long courseId) {
        //约束校验
        CoursePublishPre coursePublishPre = coursePublishPreMapper.selectById(courseId);
        if (coursePublishPre == null) {
            XueChengPlusException.cast("请先提交课程审核，审核通过才可以发布");
        }
        if (!coursePublishPre.getCompanyId().equals(companyId)) {
            XueChengPlusException.cast("不允许提交其它机构的课程。");
        }
        if (!coursePublishPre.getStatus().equals("202004")) {
            XueChengPlusException.cast("操作失败，课程审核通过方可发布。");
        }

        //保存课程发布信息
        CoursePublish coursePublish = new CoursePublish();
        BeanUtils.copyProperties(coursePublishPre, coursePublish);
        String published = "203002";//已发布
        coursePublish.setStatus(published);
        CoursePublish coursePublishUpdate = coursePublishMapper.selectById(courseId);
        if (coursePublishUpdate == null) {
            coursePublishMapper.insert(coursePublish);
        } else {
            coursePublishMapper.updateById(coursePublish);
        }

        //更新课程基本表的发布状态
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        courseBase.setStatus(published);
        courseBaseMapper.updateById(courseBase);

        //保存信息
        MqMessage addMessage = mqMessageService.addMessage("course_publish", String.valueOf(courseId), null, null);
        if (addMessage == null) {
            XueChengPlusException.cast(CommonError.UNKOWN_ERROR);
        }

        //删除课程预发布表
        coursePublishPreMapper.deleteById(courseId);
    }

    /**
     * 生成课程HTML
     *
     * @param courseId 课程id
     * @return 文件
     */
    @Override
    public File generateCourseHtml(Long courseId) {
        File htmlFile = null;
        try {
            //配置freemarker
            Configuration configuration = new Configuration(Configuration.getVersion());
            //设置模板路径
            String classPath = this.getClass().getResource("/").getPath();
            configuration.setDirectoryForTemplateLoading(new File(classPath + "/templates/"));
            //设置字符编码
            configuration.setDefaultEncoding("utf-8");
            //根据文件名称获取模板
            Template template = configuration.getTemplate("course_template.ftl");
            //模板数据
            CoursePreviewDto coursePreviewInfo = this.getCoursePreviewInfo(courseId);
            HashMap<String, Object> map = new HashMap<>();
            map.put("model", coursePreviewInfo);

            //静态化
            String htmlString = FreeMarkerTemplateUtils.processTemplateIntoString(template, map);
            InputStream inputStream = IOUtils.toInputStream(htmlString);
            //创建静态化文件
            htmlFile = File.createTempFile("course", ".html");
            log.debug("课程静态化，生成静态文件:{}", htmlFile.getAbsolutePath());
            FileOutputStream fileOutputStream = new FileOutputStream(htmlFile);
            IOUtils.copy(inputStream, fileOutputStream);
        } catch (Exception e) {
            log.error("课程静态化异常:{}", e.toString());
            XueChengPlusException.cast("课程静态化异常");
        }
        return htmlFile;
    }

    /**
     * 上传课程HTML到 MinIO
     *
     * @param courseId 课程id
     * @param file     文件
     */
    @Override
    public void uploadCourseHtml(Long courseId, File file) {
        MultipartFile multipartFile = MultipartSupportConfig.getMultipartFile(file);
        String upload = mediaServiceClient.upload(multipartFile, "course/" + courseId + ".html");
        if (upload == null) {
            XueChengPlusException.cast("上传静态文件异常");
        }
    }

    /**
     * 查询发布的课程
     *
     * @param courseId 课程id
     * @return <p>
     */
    public CoursePublish getCoursePublish(Long courseId) {
        return coursePublishMapper.selectById(courseId);
    }
}
