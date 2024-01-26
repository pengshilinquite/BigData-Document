package controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author pengshilin
 * @date 2023/2/26 14:07
 */
@Controller
public class protalController {
    @RequestMapping("/")
    public String protal(){
        return "index";
    }
}
