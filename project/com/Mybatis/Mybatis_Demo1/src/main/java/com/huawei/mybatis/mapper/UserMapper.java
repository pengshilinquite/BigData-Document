package com.huawei.mybatis.mapper;


import com.huawei.mybatis.pojo.User;

import java.util.List;

public interface UserMapper {
    /**
     * 添加用户信息
     *
     */
    int insertUser();

    /**
     * 修改用户信息
     */
    int updateUser();

    /**
     * 查询数据
     */
    User selectUser();

    /**
     * 查询所有的用户信息
     */
    List<User> selectAllUser();

}
