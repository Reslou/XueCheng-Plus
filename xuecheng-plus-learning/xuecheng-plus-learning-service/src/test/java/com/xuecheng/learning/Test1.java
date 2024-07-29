package com.xuecheng.learning;

import com.xuecheng.learning.mapper.XcChooseCourseMapper;
import org.junit.jupiter.api.Test;

/**
 * @author Mr.M
 * @version 1.0
 * @description TODO
 * @date 2022/10/2 10:32
 */
//@SpringBootTest
public class Test1 {

    //@Autowired
    XcChooseCourseMapper xcChooseCourseMapper;

    @Test
    public void test() {
        int result = 0;
        String courseTables = null;
        boolean isExpires = false;
        if (courseTables == null) {
            result = 1;
            System.out.println("result = " + result);
        } else if (isExpires == true) {
            result = 2;
            System.out.println("result = " + result);

        } else {
            result = 3;
            System.out.println("result = " + result);
        }
    }
}
