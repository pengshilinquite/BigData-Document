import com.popj.Student;
import com.popj.TestList;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class TestSpring {
    @Test
    public void Test(){
        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("applicationContext.xml");
        Student student = applicationContext.getBean("student", Student.class);
        System.out.println(student);
    }

    @Test
    public void TestSpring2(){
        ClassPathXmlApplicationContext Context = new ClassPathXmlApplicationContext("applicationContext.xml");
        TestList testList = Context.getBean("testList", TestList.class);
        System.out.println(testList);
    }
    @Test
    public void Test2() throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection root = DriverManager.getConnection("192.168.31.182", "root", "123");
        System.out.println(root);

    }
}
