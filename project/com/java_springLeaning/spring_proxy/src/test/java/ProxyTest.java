import com.huawei.spring.proxy.Calculator;
import com.huawei.spring.proxy.CalculatorImpl;
import com.huawei.spring.proxy.ProxyFactory;
import org.junit.Test;

public class ProxyTest {
    @Test
    public void testProxy(){
//        CalculatorStaticPorxy calculatorStaticPorxy = new CalculatorStaticPorxy(new CalculatorImpl());
//        int add = calculatorStaticPorxy.add(1, 2);
//        System.out.println(add);
        ProxyFactory proxyFactory = new ProxyFactory(new CalculatorImpl());
        Calculator proxy = (Calculator) proxyFactory.getProxy();
        proxy.add(1,2);


    }
}
