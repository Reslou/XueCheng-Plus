package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.mapper.CourseTeacherMapper;
import com.xuecheng.content.model.dto.AddCourseTeacherDTO;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.po.CourseTeacher;
import com.xuecheng.content.service.CourseBaseInfoService;
import com.xuecheng.content.service.CourseTeacherService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 课程老师业务层实现类
 *
 * @author zhangyang
 * @date 2024/07/15
 */
@Service
@RequiredArgsConstructor
public class CourseTeacherServiceImpl implements CourseTeacherService {

    private final CourseTeacherMapper courseTeacherMapper;
    private final CourseBaseInfoService courseBaseInfoService;

    /**
     * 列出课程教师
     *
     * @param courseId 课程id
     * @return 列表<课程教师>
     */
    @Override
    public List<CourseTeacher> listCourseTeacher(Long courseId) {
        return courseTeacherMapper
                .selectList(new LambdaQueryWrapper<CourseTeacher>()
                        .eq(CourseTeacher::getCourseId, courseId));
    }

    /**
     * 添加教师
     *
     * @param teacherDTO 教师dto
     * @return <p>
     */
    @Transactional
    @Override
    public CourseTeacher addTeacher(AddCourseTeacherDTO teacherDTO, Long companyId) {
        checkCompanyId(teacherDTO.getCourseId(), companyId);
        CourseTeacher courseTeacher = new CourseTeacher();
        BeanUtils.copyProperties(teacherDTO, courseTeacher);
        courseTeacher.setCreateDate(LocalDateTime.now());
        courseTeacherMapper.insert(courseTeacher);
        return courseTeacher;
    }

    /**
     * 校验公司id
     *
     * @param courseId  课程id
     * @param companyId 公司标识
     */
    private void checkCompanyId(Long courseId, Long companyId) {
        CourseBaseInfoDto courseBaseInfo = courseBaseInfoService.getCourseBaseInfo(courseId);
        if (!courseBaseInfo.getCompanyId().equals(companyId)) {
            throw new XueChengPlusException("只允许操作本机构的教师");
        }
    }

    /**
     * 修改教师
     *
     * @param courseTeacher 课程教师
     * @return <p>
     */
    @Transactional
    @Override
    public CourseTeacher editTeacher(CourseTeacher courseTeacher) {
        CourseTeacher oldCourseTeacher = courseTeacherMapper.selectById(courseTeacher.getId());
        LocalDateTime createDate = oldCourseTeacher.getCreateDate();
        BeanUtils.copyProperties(courseTeacher, oldCourseTeacher);
        oldCourseTeacher.setCreateDate(createDate);
        courseTeacherMapper.updateById(oldCourseTeacher);
        return oldCourseTeacher;
    }

    /**
     * 删除教师
     *
     * @param courseId  课程id
     * @param teacherId 教师id
     */
    @Override
    public void deleteTeacher(Long courseId, Long teacherId, Long companyId) {
        checkCompanyId(courseId, companyId);
        courseTeacherMapper.deleteById(teacherId);
    }
}
