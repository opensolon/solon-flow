package features.flow.stateful;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.noear.solon.Utils;
import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.FlowEngine;
import org.noear.solon.flow.Node;
import org.noear.solon.flow.stateful.*;
import org.noear.solon.flow.stateful.controller.ActorStateController;
import org.noear.solon.flow.stateful.driver.StatefulSimpleFlowDriver;
import org.noear.solon.flow.stateful.repository.InMemoryStateRepository;

import java.util.Collection;

@Slf4j
public class ActorStateFlowTest {
    final String instanceId = Utils.uuid();
    final int amount = 900000;
    final String chainId = "test1";

    @Test
    public void case1() {
        FlowEngine flowEngine = FlowEngine.newInstance(StatefulSimpleFlowDriver.builder()
                .stateController(new ActorStateController("role"))
                .stateRepository(new InMemoryStateRepository() {
                    @Override
                    public void putState(FlowContext context, Node node, StateType state) {
                        super.putState(context, node, state);
                        //todo: 打印放这儿，顺序更真实
                        log.info("{} {} 完成", node.getId(), node.getTitle());
                    }
                })
                .build());

        flowEngine.load("classpath:flow/stateful/*.yml");

        FlowStatefulService statefulService = flowEngine.statefulService();


        /// ////////////

        FlowContext context;
        StatefulTask statefulNode;


        context = getFlowContext("employee");
        statefulNode = statefulService.getTask(chainId, context);
        Assertions.assertEquals("n0", statefulNode.getNode().getId());
        Assertions.assertEquals(StateType.WAITING, statefulNode.getState());
        statefulService.postOperation(context, statefulNode.getNode(), Operation.FORWARD);


        context = getFlowContext("tl");
        statefulNode = statefulService.getTask(chainId, context);
        Assertions.assertEquals("n1", statefulNode.getNode().getId());
        Assertions.assertEquals(StateType.WAITING, statefulNode.getState());
        statefulService.postOperation(context, statefulNode.getNode(), Operation.FORWARD);


        context = getFlowContext("dm");
        Collection<StatefulTask> statefulNodes = statefulService.getTasks(chainId, context);
        for (StatefulTask auditNode : statefulNodes) {
            context = getFlowContext("dm");
            context.put("amount", amount);
            statefulService.postOperation(context, auditNode.getNode(), Operation.FORWARD);
        }

        context = getFlowContext("oa");
        statefulNode = statefulService.getTask(chainId, context);
        Assertions.assertNull(statefulNode, "必须为End节点");

    }

    private FlowContext getFlowContext(String role) {
        return FlowContext.of(instanceId).put("role", role).put("amount", amount);
    }

    private Collection<StatefulTask> getEmailNode(FlowStatefulService flowEngine) {
        FlowContext flowContext = getFlowContext("oa");
        return flowEngine.getTasks(chainId, flowContext);
    }
}
