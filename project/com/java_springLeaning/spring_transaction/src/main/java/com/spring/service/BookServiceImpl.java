package com.spring.service;

import com.spring.dao.BookDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/**
 * @author pengshilin
 * @date 2023/2/25 22:24
 */
@Service
public class BookServiceImpl implements BookService {
    @Autowired
    private BookDao bookDao;
    @Override
//    @Transactional(readOnly = true)  //用于只有查询的操作
    @Transactional(isolation = Isolation.DEFAULT)
    public void buyBook(Integer userId,Integer bookId) {
        //查询图书的价格


            Integer price =  bookDao.getPriceByBookId(bookId);
            //更新图书的库存
            bookDao.updateStock(bookId);
            //更新用户的余额
            bookDao.updateBalance(userId,price);

    }
}
