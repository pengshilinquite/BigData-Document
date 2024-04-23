package transform.RichFunction;

import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

/**
 * @author pengshilin
 * @date 2024/4/9 11:43
 */
public class RichFunctionDemo {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        env.setParallelism(2);

        DataStreamSource<Integer> integerSource = env.fromElements(1, 2, 3, 4);

        /**
         * RichMapFunction  extends AbstractRichFunction implements MapFunction<IN, OUT>
         *     RichMapFunction（抽象类） 继承了 AbstractRichFunction   同时，实现了 MapFunction接口（里面的map方法）
         *     AbstractRichFunction  里面含有 open close  （生命周期管理方法，里面含有上下文运行信息 ）
         *
         */
        integerSource.map(new RichMapFunction<Integer, Integer>() {

            /**
             *
             * @param parameters
             * @throws Exception
             * 生命周期方法，里面含有上下文
             */
            @Override
            public void open(Configuration parameters) throws Exception {
                /**
                 * 获取上下文，上下文里面含有 状态，环境运行的信息，累加器等内容
                 */
                RuntimeContext runtimeContext = getRuntimeContext();
                //获取子任务编号
                int indexOfThisSubtask = runtimeContext.getIndexOfThisSubtask();
                //获取字任务名称
                String taskNameWithSubtasks = runtimeContext.getTaskNameWithSubtasks();

                System.out.println("子任务编号："+indexOfThisSubtask+"  子任务名称："+taskNameWithSubtasks+"  调用open方法");


            }

            @Override
            public void close() throws Exception {
                /**
                 * 获取上下文，上下文里面含有 状态，环境运行的信息，累加器等内容
                 */
                RuntimeContext runtimeContext = getRuntimeContext();
                //获取子任务编号
                int indexOfThisSubtask = runtimeContext.getIndexOfThisSubtask();
                //获取字任务名称
                String taskNameWithSubtasks = runtimeContext.getTaskNameWithSubtasks();

                System.out.println("子任务编号："+indexOfThisSubtask+"  子任务名称："+taskNameWithSubtasks+"  调用close方法");
            }

            @Override
            public Integer map(Integer value) throws Exception {
                return value+1;
            }
        }).print();

        env.execute();
    }
}
