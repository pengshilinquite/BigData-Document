package com.spring.service;

import com.spring.dao.BookDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author pengshilin
 * @date 2023/2/26 0:26
 */
@Service
public class CheckServiceImpl implements CheckService {
    @Autowired
    private BookService bookService;
    @Override
    @Transactional
    public void buyBook(Integer userId, Integer[] bookIds) {
        for (Integer bookId:bookIds){
            bookService.buyBook(userId,bookId);
        }

    }
}
