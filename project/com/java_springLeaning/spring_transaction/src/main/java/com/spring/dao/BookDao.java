package com.spring.dao;

/**
 * @author pengshilin
 * @date 2023/2/25 22:26
 */
public interface BookDao {
    /**
     * 查询图书价格
     * @param bookId
     * @return
     */
    Integer getPriceByBookId(Integer bookId);

    /**
     * 更新图书库存
     * @param bookId
     */
    void updateStock(Integer bookId);

    /**
     * 更新账户余额
     * @param userId
     * @param price
     */
    void updateBalance(Integer userId, Integer price);
}
