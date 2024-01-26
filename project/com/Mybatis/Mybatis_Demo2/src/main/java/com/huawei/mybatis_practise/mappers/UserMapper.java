package com.huawei.mybatis_practise.mappers;

import com.huawei.mybatis_practise.popj.User;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface UserMapper {
    //查询返回多条记录用 List集合对象
    List<User> selectAllUser();
    //如果返回一条记录，可以选择对象，也可以选择集合
    User selectUser(String empno, String ename);
    //传入Map结构
    User selectMapUser(Map<String, Object> map);
    //插入数据
    int insertUser(User user);
    //用参数进行传参
    User selectUserParam(@Param("empno") Integer no, @Param("ename") String str2);
     //查询数量
    int getCount(@Param("empno") int no);
    //想返回Map结构
    @MapKey("empno")
    Map<String,Object> selectUserParaToMap(@Param("empno") int no);
    //模糊查询
    List<User> selectByLike(@Param("ename")String name);

    /**
     * 批量删除
     */
    int deleteIn(@Param("empno") String empno);

    /**
     * 查询指定表中的数据
     */

    List<User> getUerByTableName(@Param("tablename")String table_name);



}