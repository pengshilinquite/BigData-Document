package com.spring.aop.annotation;

import org.springframework.stereotype.Controller;

/**
 * @author pengshilin
 * @date 2023/2/25 18:54
 */
@Controller
public class TestImpl implements Test2 {
    public void save() {
        System.out.println("sdkjfsj");
    }
}
