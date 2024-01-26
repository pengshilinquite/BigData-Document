package com.spring.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author pengshilin
 * @date 2023/2/25 22:26
 */
@Repository
public class BookDaoImpl implements BookDao {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Override
    public Integer getPriceByBookId(Integer bookId) {
        String sql = "select price from t_book where book_id =?";
        Integer integer = jdbcTemplate.queryForObject(sql, Integer.class, bookId);
        return integer;
    }

    @Override
    public void updateStock(Integer bookId) {
        String sql = "update t_book set stock =  stock - 1 where book_id=?";
        jdbcTemplate.update(sql,bookId);

    }

    @Override
    public void updateBalance(Integer userId, Integer price) {
        String sql = "update t_user set balance = balance - ? where user_id = ?";
        jdbcTemplate.update(sql,price,userId);

    }
}
