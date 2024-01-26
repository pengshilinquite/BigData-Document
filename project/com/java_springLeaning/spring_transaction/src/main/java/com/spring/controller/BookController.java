package com.spring.controller;

import com.spring.service.BookService;
import com.spring.service.CheckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * @author pengshilin
 * @date 2023/2/25 22:24
 */
@Controller
public class BookController {
    @Autowired
    private BookService bookService;
    @Autowired
    private CheckService checkService;

    public void buyBook(Integer userId,Integer bookId){
            bookService.buyBook(userId,bookId);
    }

    public void checkOut(Integer userId,Integer[] bookIds){
        checkService.buyBook(userId,bookIds);
    }

}
