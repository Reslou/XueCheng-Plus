package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.mapper.*;
import com.xuecheng.content.model.dto.AddCourseDto;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.EditCourseDto;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.*;
import com.xuecheng.content.service.CourseBaseInfoService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 课程基础信息服务
 *
 * @author zhangyang
 * @date 2024/07/12
 */
@Service
@RequiredArgsConstructor
public class CourseBaseInfoServiceImpl implements CourseBaseInfoService {

    private final CourseBaseMapper courseBaseMapper;
    private final CourseMarketMapper courseMarketMapper;
    private final CourseCategoryMapper courseCategoryMapper;
    private final TeachplanMapper teachplanMapper;
    private final CourseTeacherMapper courseTeacherMapper;


    /**
     * 查询课程库列表
     *
     * @param pageParams           页面参数
     * @param queryCourseParamsDto 查询课程参数
     * @return 页面结果<course base>
     */
    @Override
    public PageResult<CourseBase> queryCourseBaseList(Long companyId, PageParams pageParams, QueryCourseParamsDto queryCourseParamsDto) {
        //获取查询参数
        Long pageSize = pageParams.getPageSize();
        Long pageNo = pageParams.getPageNo();
        String courseName = queryCourseParamsDto.getCourseName();
        String auditStatus = queryCourseParamsDto.getAuditStatus();
        String publishStatus = queryCourseParamsDto.getPublishStatus();
        //构建查询条件
        LambdaQueryWrapper<CourseBase> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper
                .like(StringUtils.isNotEmpty(courseName), CourseBase::getName, courseName)
                .eq(StringUtils.isNotEmpty(auditStatus), CourseBase::getAuditStatus, auditStatus)
                .eq(StringUtils.isNotEmpty(publishStatus), CourseBase::getStatus, publishStatus)
                .eq(CourseBase::getCompanyId,companyId);
        //构建分页条件
        Page<CourseBase> page = new Page<>(pageNo, pageSize);
        //分页查询
        page = courseBaseMapper.selectPage(page, queryWrapper);
        return new PageResult<>(page.getRecords(), page.getTotal(), pageNo, pageSize);
    }

    /**
     * 新增课程基地
     *
     * @param companyId 公司标识
     * @param dto       添加课程dto
     * @return 课程基础信息
     */
    @Transactional
    @Override
    public CourseBaseInfoDto createCourseBase(Long companyId, AddCourseDto dto) {
        //合法性校验
        //check(dto);
        //补充数据并保存课程基本信息表
        CourseBase courseBase = new CourseBase();
        BeanUtils.copyProperties(dto, courseBase);
        courseBase.setAuditStatus("202002");
        courseBase.setStatus("203001");
        courseBase.setCompanyId(companyId);
        courseBase.setCreateDate(LocalDateTime.now());
        int insert = courseBaseMapper.insert(courseBase);
        if (insert <= 0) {
            throw new RuntimeException("新增课程基本信息失败");
        }
        //保存课程营销信息
        CourseMarket courseMarket = new CourseMarket();
        BeanUtils.copyProperties(dto, courseMarket);
        Long courseId = courseBase.getId();
        courseMarket.setId(courseId);
        int i = saveCourseMarket(courseMarket);
        if (i <= 0) {
            throw new RuntimeException("保存课程营销信息失败");
        }
        //查询课程基本信息及营销信息
        return getCourseBaseInfo(courseId);
    }

    /**
     * 查询课程基本信息及营销信息
     *
     * @param courseId 进程id
     * @return 课程基础信息
     */
    public CourseBaseInfoDto getCourseBaseInfo(Long courseId) {
        //查询课程基本信息和营销信息
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        if (courseBase == null) {
            return null;
        }
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);
        CourseBaseInfoDto courseBaseInfoDto = new CourseBaseInfoDto();
        BeanUtils.copyProperties(courseBase, courseBaseInfoDto);
        if (courseMarket != null) {
            BeanUtils.copyProperties(courseMarket, courseBaseInfoDto);
        }
        //查询分类名称
        CourseCategory st = courseCategoryMapper.selectById(courseBase.getSt());//一级分类
        courseBaseInfoDto.setStName(st.getName());
        CourseCategory mt = courseCategoryMapper.selectById(courseBase.getMt());//二级分类
        courseBaseInfoDto.setMtName(mt.getName());
        return courseBaseInfoDto;
    }

    /**
     * 更新课程
     *
     * @param companyId     公司标识
     * @param editCourseDto 修改课程dto
     * @return 课程基础信息
     */
    @Transactional
    @Override
    public CourseBaseInfoDto updateCourseBase(Long companyId, EditCourseDto editCourseDto) {
        Long courseId = editCourseDto.getId();
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        if (courseBase == null) {
            XueChengPlusException.cast("课程不存在");
        }
        if (!courseBase.getCompanyId().equals(companyId)) {
            XueChengPlusException.cast("只能修改本机构的课程");
        }
        BeanUtils.copyProperties(editCourseDto, courseBase);
        courseBase.setChangeDate(LocalDateTime.now());
        int i = courseBaseMapper.updateById(courseBase);
        if (i <= 0) {
            XueChengPlusException.cast("更新失败");
        }
        CourseMarket courseMarket = new CourseMarket();
        BeanUtils.copyProperties(editCourseDto, courseMarket);
        saveCourseMarket(courseMarket);
        return getCourseBaseInfo(courseId);
    }

    /**
     * 删除课程
     *
     * @param companyId 公司标识
     * @param courseId  课程id
     */
    @Transactional
    @Override
    public void deleteCourse(Long companyId, Long courseId) {
        CourseBaseInfoDto courseBase = getCourseBaseInfo(courseId);
        if (!courseBase.getCompanyId().equals(companyId)) {
            XueChengPlusException.cast("只能修改本机构的课程");
        }
        if (courseBase.getAuditStatus().equals("203001")) {
            XueChengPlusException.cast("只能删除未发布的课程");
        }

        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);
        if (courseMarket != null) {
            courseMarketMapper.deleteById(courseMarket);
        }

        List<Teachplan> teachplanList = teachplanMapper
                .selectList(new LambdaQueryWrapper<Teachplan>()
                        .eq(Teachplan::getCourseId, courseId));
        if (!teachplanList.isEmpty()) {
            for (Teachplan teachplan : teachplanList) {
                teachplanMapper.deleteById(teachplan.getId());
            }
        }

        List<CourseTeacher> courseTeacherList = courseTeacherMapper
                .selectList(new LambdaQueryWrapper<CourseTeacher>()
                        .eq(CourseTeacher::getCourseId, courseId));
        if (!courseTeacherList.isEmpty()) {
            for (CourseTeacher courseTeacher : courseTeacherList) {
                teachplanMapper.deleteById(courseTeacher.getId());
            }
        }

        courseBaseMapper.deleteById(courseId);
    }


    /**
     * 保存课程营销信息
     *
     * @param courseMarket 课程营销信息
     * @return int 课程营销信息处理结果
     */
    private int saveCourseMarket(CourseMarket courseMarket) {
        //校验收费
        String charge = courseMarket.getCharge();
        if (StringUtils.isBlank(charge)) {
            throw new RuntimeException("收费规则没有选择");
        }
        if (charge.equals("201001")) {
            Float price = courseMarket.getPrice();
            if (price == null || price <= 0) {
                throw new RuntimeException("课程收费价格不能为空且必须大于0");
            }
        }
        //根据表中有无数据处理
        CourseMarket select = courseMarketMapper.selectById(courseMarket.getId());
        if (select == null) {
            return courseMarketMapper.insert(courseMarket);
        } else {
            BeanUtils.copyProperties(courseMarket, select);
            select.setId(courseMarket.getId());
            return courseMarketMapper.updateById(select);
        }
    }

    /**
     * 合法性校验
     *
     * @param dto 添加课程dto
     */
    private static void check(AddCourseDto dto) {
        if (StringUtils.isBlank(dto.getName())) {
            // throw new RuntimeException("课程名称为空");
            XueChengPlusException.cast("课程名称为空");
        }
        if (StringUtils.isBlank(dto.getMt())) {
            throw new RuntimeException("课程分类为空");
        }
        if (StringUtils.isBlank(dto.getSt())) {
            throw new RuntimeException("课程分类为空");
        }
        if (StringUtils.isBlank(dto.getGrade())) {
            throw new RuntimeException("课程等级为空");
        }
        if (StringUtils.isBlank(dto.getTeachmode())) {
            throw new RuntimeException("教育模式为空");
        }
        if (StringUtils.isBlank(dto.getUsers())) {
            throw new RuntimeException("适应人群为空");
        }
        if (StringUtils.isBlank(dto.getCharge())) {
            throw new RuntimeException("收费规则为空");
        }
    }
}
