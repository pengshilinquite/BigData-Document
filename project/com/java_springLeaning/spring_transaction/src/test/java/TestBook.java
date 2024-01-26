import com.spring.controller.BookController;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author pengshilin
 * @date 2023/2/25 22:49
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:spring-jdbc.xml")
public class TestBook {
    @Autowired
    private BookController bookController;
    @Test
    public void bookTest(){
        bookController.checkOut(1,new Integer[]{1,2});

    }
}
