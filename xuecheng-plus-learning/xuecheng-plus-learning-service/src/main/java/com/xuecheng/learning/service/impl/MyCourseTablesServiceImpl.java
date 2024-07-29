package com.xuecheng.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.learning.feignclient.ContentServiceClient;
import com.xuecheng.learning.mapper.XcChooseCourseMapper;
import com.xuecheng.learning.mapper.XcCourseTablesMapper;
import com.xuecheng.learning.model.dto.MyCourseTableParams;
import com.xuecheng.learning.model.dto.XcChooseCourseDto;
import com.xuecheng.learning.model.dto.XcCourseTablesDto;
import com.xuecheng.learning.model.po.XcChooseCourse;
import com.xuecheng.learning.model.po.XcCourseTables;
import com.xuecheng.learning.service.MyCourseTablesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class MyCourseTablesServiceImpl implements MyCourseTablesService {
    private final ContentServiceClient contentServiceClient;
    private final XcChooseCourseMapper xcChooseCourseMapper;
    private final XcCourseTablesMapper xcCourseTablesMapper;

    /**
     * 添加选课记录
     *
     * @param userId   用户id
     * @param courseId 课程id
     * @return 选课记录Dto
     */
    @Override
    public XcChooseCourseDto addChooseCourse(String userId, Long courseId) {
        //查询发布的课程
        CoursePublish coursePublish = contentServiceClient.getCoursepublish(courseId);
        XcChooseCourse chooseCourse;
        if ("201000".equals(coursePublish.getCharge())) {
            //添加免费课程
            chooseCourse = addChooseCourse(userId, coursePublish, "700001", "701001");
            //添加到我的课程表
            XcCourseTables courseTables = addCourseTable(chooseCourse);
        } else {
            //添加收费课程
            chooseCourse = addChooseCourse(userId, coursePublish, "700002", "701002");
        }
        //获取学习资格并封装返回
        XcChooseCourseDto xcChooseCourseDto = new XcChooseCourseDto();
        BeanUtils.copyProperties(chooseCourse,xcChooseCourseDto);
        XcCourseTablesDto learningStatus = getLearningStatus(userId, courseId);
        xcChooseCourseDto.setLearnStatus(learningStatus.getLearnStatus());
        return xcChooseCourseDto;
    }




    /**
     * 添加选课记录
     *
     * @param userId        用户id
     * @param coursePublish 发布的课程
     * @return <p> 选课记录
     */
    private XcChooseCourse addChooseCourse(
            String userId, CoursePublish coursePublish, String orderType, String status) {
        //查询选课记录
        Long courseId = coursePublish.getId();
        List<XcChooseCourse> xcChooseCourses = xcChooseCourseMapper
                .selectList(new LambdaQueryWrapper<XcChooseCourse>()
                .eq(XcChooseCourse::getUserId, userId)
                .eq(XcChooseCourse::getCourseId, courseId)
                .eq(XcChooseCourse::getOrderType, orderType)//免费课程
                .eq(XcChooseCourse::getStatus, status));//选课成功
        if (xcChooseCourses != null && !xcChooseCourses.isEmpty()) {
            return xcChooseCourses.get(0);
        }
        //设置课程有效时间和课程价格
        int plusDays = 365;
        float coursePrice = 0f;
        if (orderType.equals("700002")) {
            plusDays = coursePublish.getValidDays();
            coursePrice = coursePublish.getPrice();
        }
        //添加选课记录
        XcChooseCourse xcChooseCourse = XcChooseCourse.builder()
                .courseId(courseId)
                .courseName(coursePublish.getName())
                .coursePrice(coursePrice)
                .userId(userId)
                .companyId(coursePublish.getCompanyId())
                .orderType(orderType)
                .createDate(LocalDateTime.now())
                .status(status)
                .validDays(plusDays)
                .validtimeStart(LocalDateTime.now())
                .validtimeEnd(LocalDateTime.now().plusDays(plusDays))
                .build();
        xcChooseCourseMapper.insert(xcChooseCourse);
        return xcChooseCourse;
    }

    /**
     * 添加我的课程表
     *
     * @param chooseCourse 选课记录
     * @return <p> 我的课程表
     */
    private XcCourseTables addCourseTable(XcChooseCourse chooseCourse) {
        if (!"701001".equals(chooseCourse.getStatus())) {
            XueChengPlusException.cast("选课未成功，无法添加到课程表");
        }
        //查询我的课程表
        XcCourseTables courseTables = getXcCourseTables(chooseCourse.getUserId(), chooseCourse.getCourseId());
        if (courseTables != null) {
            return courseTables;
        }
        //添加我的课程表
        XcCourseTables courseTablesNew = XcCourseTables.builder()
                .chooseCourseId(chooseCourse.getId())
                .courseId(chooseCourse.getCourseId())
                .userId(chooseCourse.getUserId())
                .companyId(chooseCourse.getCompanyId())
                .courseName(chooseCourse.getCourseName())
                .validtimeStart(chooseCourse.getValidtimeStart())
                .validtimeEnd(chooseCourse.getValidtimeEnd())
                .createDate(LocalDateTime.now())
                .updateDate(LocalDateTime.now())
                .courseType(chooseCourse.getOrderType())
                .build();
        xcCourseTablesMapper.insert(courseTablesNew);
        return courseTablesNew;
    }

    /**
     * 获取学习资格
     *
     * @param userId   用户id
     * @param courseId 课程id
     * @return 课程表Dto
     */
    @Override
    public XcCourseTablesDto getLearningStatus(String userId, Long courseId) {
        XcCourseTablesDto xcCourseTablesDto = new XcCourseTablesDto();
        //查询我的课程表
        XcCourseTables xcCourseTables = getXcCourseTables(userId, courseId);
        if (xcCourseTables==null){
            xcCourseTablesDto.setLearnStatus("702002");//没有选课或选课后没有支付
            return xcCourseTablesDto;
        }
        BeanUtils.copyProperties(xcCourseTables,xcCourseTablesDto);
        //是否过期
        boolean isExpires = xcCourseTables.getValidtimeEnd().isBefore(LocalDateTime.now());
        if (!isExpires){
            xcCourseTablesDto.setLearnStatus("702001");
        }else {
            xcCourseTablesDto.setLearnStatus("702003");
        }
        return xcCourseTablesDto;
    }

    /**
     * 保存选择课程成功
     *
     * @param chooseCourseId 选择课程编号
     * @return 保存是否成功
     */
    @Override
    public boolean saveChooseCourseSuccess(String chooseCourseId) {
        //查询选课记录
        XcChooseCourse chooseCourse = xcChooseCourseMapper.selectById(chooseCourseId);
        if (chooseCourse==null){
            log.debug("收到支付结果通知没有查询到关联的选课记录,选课ID:{}",chooseCourseId);
        }
        //添加到我的课程表
        String status = null;
        if (chooseCourse != null) {
            status = chooseCourse.getStatus();
        }
        if("701001".equals(status)){//状态为选课成功
            addCourseTable(chooseCourse);
            return true;
        } 
        if ("701002".equals(status)){//状态为已支付
            chooseCourse.setStatus("701001");
            int update = xcChooseCourseMapper.updateById(chooseCourse);
            if (update>0){
                log.debug("收到支付结果通知处理成功,选课记录:{}",chooseCourse);
                addCourseTable(chooseCourse);
                return true;
            }else {
                log.debug("收到支付结果通知处理失败,选课记录:{}",chooseCourse);
                return false;
            }
        }
        return false;
    }
    /**
     * 我的课程表
     *
     * @param params 我的课程表参数
     * @return 分页结果<课程表>
     */
    public PageResult<XcCourseTables> myCourseTables(MyCourseTableParams params){
        //页码
        long pageNo = params.getPage();
        //每页记录数
        long pageSize = params.getSize();
        //分页查询
        Page<XcCourseTables> pageResult = xcCourseTablesMapper.selectPage(new Page<>(pageNo, pageSize),
                        new LambdaQueryWrapper<XcCourseTables>()
                                .eq(XcCourseTables::getUserId, params.getUserId()));
        return new PageResult<>(pageResult.getRecords(), pageResult.getTotal(), pageNo, pageSize);
    }

    /**
     * 获取xc课程表
     *
     * @param userId   用户id
     * @param courseId 课程id
     * @return <p>
     */
    private XcCourseTables getXcCourseTables(String userId,Long courseId) {
        return xcCourseTablesMapper.selectOne(new LambdaQueryWrapper<XcCourseTables>()
                .eq(XcCourseTables::getUserId, userId)
                .eq(XcCourseTables::getCourseId, courseId));
    }
}
