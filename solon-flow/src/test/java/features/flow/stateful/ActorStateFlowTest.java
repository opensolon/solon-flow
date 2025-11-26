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
import org.noear.solon.flow.stateful.repository.InMemoryStateRepository;

import java.util.Collection;

@Slf4j
public class ActorStateFlowTest {
    final String instanceId = Utils.uuid();
    final int amount = 900000;
    final String graphId = "test1";

    ActorStateController stateController = new ActorStateController("role");
    InMemoryStateRepository stateRepository = new InMemoryStateRepository() {
        @Override
        public void statePut(FlowContext context, Node node, StateType state) {
            super.statePut(context, node, state);
            //todo: 打印放这儿，顺序更真实
            if (state == StateType.COMPLETED) {
                log.info("{} {} 完成", node.getId(), node.getTitle());
            }
        }
    };

    @Test
    public void case1() {
        FlowEngine flowEngine = FlowEngine.newInstance();

        flowEngine.load("classpath:flow/stateful/*.yml");

        FlowStatefulService statefulService = flowEngine.forStateful();


        /// ////////////

        FlowContext context;
        StatefulTask statefulNode;


        context = getFlowContext("employee");
        statefulNode = statefulService.getTask(graphId, context);
        Assertions.assertEquals("n0", statefulNode.getNode().getId());
        Assertions.assertEquals(StateType.WAITING, statefulNode.getState());
        statefulService.postOperation(statefulNode.getNode(), Operation.FORWARD, context);


        context = getFlowContext("tl");
        statefulNode = statefulService.getTask(graphId, context);
        Assertions.assertEquals("n1", statefulNode.getNode().getId());
        Assertions.assertEquals(StateType.WAITING, statefulNode.getState());
        statefulService.postOperation(statefulNode.getNode(), Operation.FORWARD, context);


        context = getFlowContext("dm");
        Collection<StatefulTask> statefulNodes = statefulService.getTasks(graphId, context);
        for (StatefulTask auditNode : statefulNodes) {
            context = getFlowContext("dm");
            context.put("amount", amount);
            statefulService.postOperation(auditNode.getNode(), Operation.FORWARD, context);
        }

        context = getFlowContext("oa");
        statefulNode = statefulService.getTask(graphId, context);
        Assertions.assertNull(statefulNode, "必须为End节点");

    }

    private FlowContext getFlowContext(String role) {
        return FlowContext.of(instanceId, stateController, stateRepository).put("role", role).put("amount", amount);
    }

    private Collection<StatefulTask> getEmailNode(FlowStatefulService flowEngine) {
        FlowContext flowContext = getFlowContext("oa");
        return flowEngine.getTasks(graphId, flowContext);
    }
}