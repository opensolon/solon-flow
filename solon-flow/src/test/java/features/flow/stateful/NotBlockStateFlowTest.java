package features.flow.stateful;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.noear.solon.flow.*;
import org.noear.solon.flow.intercept.ChainInterceptor;
import org.noear.solon.flow.intercept.ChainInvocation;
import org.noear.solon.flow.stateful.FlowStatefulService;
import org.noear.solon.flow.stateful.StateType;
import org.noear.solon.flow.stateful.StatefulTask;
import org.noear.solon.flow.stateful.controller.NotBlockStateController;
import org.noear.solon.flow.stateful.repository.InMemoryStateRepository;

@Slf4j
public class NotBlockStateFlowTest {
    final int amount = 900000;
    final String chainId = "test1";

    NotBlockStateController stateController = new NotBlockStateController();
    InMemoryStateRepository stateRepository = new InMemoryStateRepository() {
        @Override
        public void statePut(FlowContext context, Node node, StateType state) {
            super.statePut(context, node, state);
            //todo: 打印放这儿，顺序更真实
            log.info("{} {} 完成", node.getId(), node.getTitle());
        }
    };

    @Test
    public void case1() {
        FlowEngine flowEngine = FlowEngine.newInstance();

        flowEngine.load("classpath:flow/stateful/*.yml");

        FlowStatefulService statefulService = flowEngine.statefulService();


        /// ////////////

        FlowContext context = FlowContext.of("1", stateController, stateRepository)
                .put("amount", amount);
        StatefulTask statefulNode;


        statefulNode = statefulService.getTask(chainId, context);

        Assertions.assertEquals("n5", statefulNode.getNode().getId());
        Assertions.assertEquals(StateType.COMPLETED, statefulNode.getState());
    }

    @Test
    public void caseInterceptorBlock() {
        ChainInterceptor interceptor = new ChainInterceptor() {
            @Override
            public void doIntercept(ChainInvocation invocation) throws FlowException {
                invocation.invoke();
            }

            @Override
            public void onNodeStart(FlowExchanger exchanger, Node node) {
                if (node.getId().equals("n1")) {
                    exchanger.interrupt();
                }
            }
        };
        FlowEngine flowEngine = FlowEngine.newInstance();
        flowEngine.addInterceptor(interceptor);

        flowEngine.load("classpath:flow/stateful/*.yml");

        FlowStatefulService statefulService = flowEngine.statefulService();


        /// ////////////

        FlowContext context = FlowContext.of("2", stateController, stateRepository)
                .put("amount", amount);
        StatefulTask statefulNode;


        statefulNode = statefulService.getTask(chainId, context);

        Assertions.assertEquals("n0", statefulNode.getNode().getId());
        Assertions.assertEquals(StateType.COMPLETED, statefulNode.getState());


        statefulNode = statefulService.getTask(chainId, context);

        Assertions.assertNull(statefulNode); //提前中断，没有节点可取了

        System.out.println("---------------------");

        flowEngine.removeInterceptor(interceptor);

        statefulNode = statefulService.getTask(chainId, context);

        Assertions.assertEquals("n5", statefulNode.getNode().getId());
        Assertions.assertEquals(StateType.COMPLETED, statefulNode.getState());


        statefulNode = statefulService.getTask(chainId, context);

        Assertions.assertNull(statefulNode); //全部完成，没有节点可取了
    }

    @Test
    public void caseTaskBlock() {
        FlowEngine flowEngine = FlowEngine.newInstance();
        FlowStatefulService statefulService = flowEngine.statefulService();
        Chain chain = getChain();

        FlowContext context = FlowContext.of("3", stateController, stateRepository)
                .put("tag", "");

        StatefulTask statefulNode = statefulService.getTask(chain, context);
        Assertions.assertNotNull(statefulNode);
        Assertions.assertEquals("n3", statefulNode.getNode().getId());
        Assertions.assertEquals(StateType.COMPLETED, statefulNode.getState());

        context = FlowContext.of("4", stateController, stateRepository)
                .put("tag", "n1");

        statefulNode = statefulService.getTask(chain, context);
        Assertions.assertNotNull(statefulNode);
        Assertions.assertEquals("n1", statefulNode.getNode().getId());
        Assertions.assertEquals(StateType.WAITING, statefulNode.getState());

        //再跑（仍在原位、原状态）
        statefulNode = statefulService.getTask(chain, context);
        Assertions.assertNotNull(statefulNode);
        Assertions.assertEquals("n1", statefulNode.getNode().getId());
        Assertions.assertEquals(StateType.WAITING, statefulNode.getState());


        context = FlowContext.of("4", stateController, stateRepository)
                .put("tag", "n2");

        statefulNode = statefulService.getTask(chain, context);
        Assertions.assertNotNull(statefulNode);
        Assertions.assertEquals("n2", statefulNode.getNode().getId());
        Assertions.assertEquals(StateType.WAITING, statefulNode.getState());

        context = FlowContext.of("4", stateController, stateRepository)
                .put("tag", "");

        statefulNode = statefulService.getTask(chain, context);
        Assertions.assertNotNull(statefulNode);
        Assertions.assertEquals("n3", statefulNode.getNode().getId());
        Assertions.assertEquals(StateType.COMPLETED, statefulNode.getState());
    }

    private Chain getChain() {
        Chain chain = new Chain("tmp-" + System.currentTimeMillis());

        String task = "if(tag.equals(node.getId())){exchanger.interrupt();}";

        chain.addNode(NodeDecl.startOf("s").linkAdd("n0"));
        chain.addNode(NodeDecl.activityOf("n0").task(task).linkAdd("n1"));
        chain.addNode(NodeDecl.activityOf("n1").task(task).linkAdd("n2"));
        chain.addNode(NodeDecl.activityOf("n2").task(task).linkAdd("n3"));
        chain.addNode(NodeDecl.activityOf("n3").task(task).linkAdd("e"));
        chain.addNode(NodeDecl.endOf("e"));

        return chain;
    }
}