package com.spring.aop.annotation;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * @author pengshilin
 * @date 2023/2/25 18:32
 */
@Component
@Aspect //将当前组件表示为切面
public class loggerAspect {
    @Pointcut("execution(* com.spring.aop.annotation.CalculatorImpl.*(..))")
    public void pointCut(){

    }

//    @Before("execution(public int com.spring.aop.annotation.CalculatorImpl.add(int ,int ))")
//    @Before("execution(* com.spring.aop.annotation.CalculatorImpl.*(..))")
      @Before("pointCut()")
    /**
     * JoinPoint 获取连接点对应的信息
     */
    public void beforeAdviceMethod(JoinPoint joinPoint){
        /**
         * 获取连接点对应的方法名
         */
        Signature signature = joinPoint.getSignature();
        /**
         * 获取连接点对应的参数
         */
        Object[] args = joinPoint.getArgs();
        System.out.println("loggerAspect,前置通知,方法："+signature.getName()+",参数："+ Arrays.toString(args));
    }
    @After("pointCut()")
    public void afterAdviceMethod(JoinPoint joinPoint){
        /**
         * 获取连接点对应的方法名
         */
        Signature signature = joinPoint.getSignature();
        /**
         * 获取连接点对应的参数
         */
        Object[] args = joinPoint.getArgs();
        System.out.println("loggerAspect,后置方法通知,方法："+signature.getName()+",参数："+ Arrays.toString(args));
    }
    @AfterReturning(value = "pointCut()" ,returning="result")
    public void afterReturnningAdviceMethod(JoinPoint joinPoint,Object result){
        System.out.println("LoggerAspect,返回通知,方法"+ joinPoint.getSignature().getName()+ ",结果："+result);
    }
    @AfterThrowing(value = "pointCut()",throwing = "ex")
    public void afterThrowingAdviceMethod(JoinPoint joinPoint,Throwable ex){
        Signature signature = joinPoint.getSignature();
        System.out.println("LoggerAspect,返回错误通知,方法"+ joinPoint.getSignature().getName()+ ",报错内容："+ex);
    }
    @Around(value = "pointCut()")
    public Object aroundAdviceMethod(ProceedingJoinPoint joinPoint){
        Object result = null;
        try {
            System.out.println("环绕通知---------->前置通知");
             result = joinPoint.proceed();
            System.out.println("环绕通知---------->返回通知");
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            System.out.println("环绕通知---------->异常通知");
        } finally {
            System.out.println("环绕通知---------->后置通知");
        }


            return result;
    }

}
