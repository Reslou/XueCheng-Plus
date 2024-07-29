package com.xuecheng.learning.model.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 
 * </p>
 *
 * @author itcast
 */
@Data
@Builder
@NoArgsConstructor
@TableName("xc_course_tables")
public class XcCourseTables implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 选课记录id
     */
    private Long chooseCourseId;

    /**
     * 用户id
     */
    private String userId;

    /**
     * 课程id
     */
    private Long courseId;

    /**
     * 机构id
     */
    private Long companyId;

    /**
     * 课程名称
     */
    private String courseName;
    /**
     * 课程名称
     */
    private String courseType;


    /**
     * 添加时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createDate;

    /**
     * 开始服务时间
     */
    private LocalDateTime validtimeStart;

    /**
     * 到期时间
     */
    private LocalDateTime validtimeEnd;

    /**
     * 更新时间
     */
    private LocalDateTime updateDate;

    /**
     * 备注
     */
    private String remarks;


    public XcCourseTables(Long id, Long chooseCourseId, String userId, Long courseId, Long companyId, String courseName, String courseType, LocalDateTime createDate, LocalDateTime validtimeStart, LocalDateTime validtimeEnd, LocalDateTime updateDate, String remarks) {
        this.id = id;
        this.chooseCourseId = chooseCourseId;
        this.userId = userId;
        this.courseId = courseId;
        this.companyId = companyId;
        this.courseName = courseName;
        this.courseType = courseType;
        this.createDate = createDate;
        this.validtimeStart = validtimeStart;
        this.validtimeEnd = validtimeEnd;
        this.updateDate = updateDate;
        this.remarks = remarks;
    }
}
