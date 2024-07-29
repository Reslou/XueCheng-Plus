package com.xuecheng.content;

import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.service.CoursePublishService;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

/**
 * freemarker测试
 *
 * @author reslou
 * @date 2024/07/22
 */
@SpringBootTest
public class FreemarkerTest {
    @Autowired
    CoursePublishService coursePublishService;

    /**
     * 测试通过模板生成HTML
     */
    @Test
    public void testGenerateHtmlByTemplate() throws IOException, TemplateException {
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
        CoursePreviewDto coursePreviewInfo = coursePublishService.getCoursePreviewInfo(117L);
        HashMap<String, Object> map = new HashMap<>();
        map.put("model", coursePreviewInfo);
        //静态化
        String htmlString = FreeMarkerTemplateUtils.processTemplateIntoString(template, map);
        InputStream inputStream = IOUtils.toInputStream(htmlString);
        FileOutputStream fileOutputStream = new FileOutputStream("E:\\code\\test.html");
        IOUtils.copy(inputStream, fileOutputStream);
    }
}
