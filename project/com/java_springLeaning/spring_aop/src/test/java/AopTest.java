import com.spring.aop.annotation.Calculator;
import com.spring.aop.annotation.Test2;
import com.spring.aop.annotation.TestImpl;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author pengshilin
 * @date 2023/2/25 18:49
 */
public class AopTest {
    @Test
    public void testAop(){
        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("applicationContext.xml");
        Calculator bean = applicationContext.getBean(Calculator.class);
        bean.div(1,1);


    }
    @Test
    public void test2(){
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("applicationContext.xml");
        Test2 bean = context.getBean(Test2.class);
        bean.save();


    }
}
