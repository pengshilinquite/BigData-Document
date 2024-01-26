
import com.spring.popj.User;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;
import java.util.Map;

/**
 * @author pengshilin
 * @date 2023/2/25 21:25
 */


//指定当前测试在spring测试环境中运行，此时就可以通过注入的方式直接获取Ioc中的bean ，不需要每次都 new ClassPathXMLApplicationContext
@RunWith(SpringJUnit4ClassRunner.class)
//设置spring配置文件
@ContextConfiguration("classpath:spring-jdbc.xml")
public class TestJdbc {
    //自动装配获取bean对象
     @Autowired
    private JdbcTemplate jdbcTemplate;
     @Test
    public void jdbcTest(){
         String sql = "insert into dm_fin_user_h_f values(?,?,?,?)";
         jdbcTemplate.update(sql,"peng","111","18","jdfso@qq.com");
    }

    @Test
    public void testQueryById(){
         String sql="select * from dm_fin_user_h_f where username=?";
         String sqlAll = "select * from dm_fin_user_h_f";
         String sqlCount = "select count(1) from dm_fin_user_h_f";
        User user = jdbcTemplate.queryForObject(sql, new BeanPropertyRowMapper<>(User.class), "root");
        List<User> query = jdbcTemplate.query(sqlAll, new BeanPropertyRowMapper<>(User.class));
        Integer integer = jdbcTemplate.queryForObject(sqlCount, Integer.class);



        System.out.println(user);
        System.out.println(query);
        System.out.println(integer);

    }
}
