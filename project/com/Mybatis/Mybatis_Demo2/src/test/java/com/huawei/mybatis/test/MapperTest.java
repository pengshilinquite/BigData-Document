package com.huawei.mybatis.test;

import com.huawei.mybatis_practise.mappers.UserMapper;
import com.huawei.mybatis_practise.popj.User;
import com.huawei.utils.SqlSessionsUtils;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.SqlSession;
import org.junit.Test;

import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapperTest {
    @Test
    public void test() throws Exception {
        SqlSession sqlSession = SqlSessionsUtils.getSqlSession();
        UserMapper mapper = sqlSession.getMapper(UserMapper.class);
//        List<User> users = mapper.selectAllUser();
//        users.forEach(user-> System.out.println(user));

/*        User peng = mapper.selectUser("8888","peng");
        System.out.println(peng);*/


/*        Map<String, Object> map = new HashMap<String, Object>();
        map.put("empno","8888");
        map.put("ename","peng");
        User user = mapper.selectMapUser(map);
        System.out.println(user);
        */


       /* User peng = mapper.selectUserParam( 8888,"peng");
        System.out.println(peng);*/

      /*  User user = new User(9999,"chen","CLERK",7782,new SimpleDateFormat("yyyy-MM-dd").parse("1996-12-03"),2800,Double.parseDouble("0"),10);

        int i = mapper.insertUser(user);
        if (i>=1){
            System.out.println("数据插入成功");
        }else {
            System.out.println("数据插入失败");
        }

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("empno","9999");
        map.put("ename","chen");


        User userSelect = mapper.selectMapUser(map);
        System.out.println(userSelect);*/

        /*int count = mapper.getCount(8888);
        System.out.println(count);*/


//        Map<String, Object> map = mapper.selectUserParaToMap(8888);
//        System.out.println(map);

//        List<User> p = mapper.selectByLike("S");
//        System.out.println(p);

       /* int i = mapper.deleteIn("9999,8888");
        System.out.println(i);*/

        List<User> uerByTableName = mapper.getUerByTableName("dm_fin.dm_fin_emp_h_f");
        System.out.println(uerByTableName);

        sqlSession.close();

    }

}
