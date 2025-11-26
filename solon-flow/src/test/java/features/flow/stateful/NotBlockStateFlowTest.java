package features.flow.stateful;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.noear.solon.flow.*;
import org.noear.solon.flow.intercept.FlowInterceptor;
import org.noear.solon.flow.intercept.FlowInvocation;
import org.noear.solon.flow.stateful.FlowStatefulService;
import org.noear.solon.flow.stateful.StateType;
import org.noear.solon.flow.stateful.StatefulTask;
import org.noear.solon.flow.stateful.controller.NotBlockStateController;
import org.noear.solon.flow.stateful.repository.InMemoryStateRepository;

@Slf4j
public class NotBlockStateFlowTest {
    final int amount = 900000;
    final String graphId = "test1";

    NotBlockStateController stateController = new NotBlockStateController();
    InMemoryStateRepository stateRepository = new InMemoryStateRepository() {
        @Override
        public void statePut(FlowContext context, Node node, StateType state) {
            super.statePut(context, node, state);
            //todo: 打印放这儿，顺序更真实
            if (state == StateType.COMPLETED) {
                log.info("{} 完成", node.getId());
            }
        }
    };

    @Test
    public void case1() {
        FlowEngine flowEngine = FlowEngine.newInstance();

        flowEngine.load("classpath:flow/stateful/*.yml");

        FlowStatefulService statefulService = flowEngine.forStateful();


        /// ////////////

        FlowContext context = FlowContext.of("1", stateController, stateRepository)
                .put("amount", amount);
        StatefulTask task;


        task = statefulService.getTask(graphId, context);

        Assertions.assertEquals("n5", task.getNode().getId());
        Assertions.assertEquals(StateType.COMPLETED, task.getState());
    }

    @Test
    public void caseInterceptorBlock() {
        FlowInterceptor interceptor = new FlowInterceptor() {
            @Override
            public void doIntercept(FlowInvocation invocation) throws FlowException {
                invocation.invoke();
            }

            @Override
            public void onNodeStart(FlowContext context, Node node) {
                if (node.getId().equals("n1")) {
                    context.exchanger().interrupt();
                }
            }
        };
        FlowEngine flowEngine = FlowEngine.newInstance();
        flowEngine.addInterceptor(interceptor);

        flowEngine.load("classpath:flow/stateful/*.yml");

        FlowStatefulService statefulService = flowEngine.forStateful();


        /// ////////////

        FlowContext context = FlowContext.of("2", stateController, stateRepository)
                .put("amount", amount);
        StatefulTask task;


        task = statefulService.getTask(graphId, context);

        Assertions.assertEquals("n0", task.getNode().getId());
        Assertions.assertEquals(StateType.COMPLETED, task.getState());


        task = statefulService.getTask(graphId, context);

        Assertions.assertNull(task); //提前中断，没有节点可取了

        System.out.println("---------------------");

        flowEngine.removeInterceptor(interceptor);

        task = statefulService.getTask(graphId, context);

        Assertions.assertEquals("n5", task.getNode().getId());
        Assertions.assertEquals(StateType.COMPLETED, task.getState());


        task = statefulService.getTask(graphId, context);

        Assertions.assertNull(task); //全部完成，没有节点可取了
    }
}