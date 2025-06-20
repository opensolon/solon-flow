package features.flow.stateful;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.noear.solon.Utils;
import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.Node;
import org.noear.solon.flow.stateful.*;
import org.noear.solon.flow.stateful.controller.ActorStateController;
import org.noear.solon.flow.stateful.driver.StatefulSimpleFlowDriver;
import org.noear.solon.flow.stateful.repository.InMemoryStateRepository;

import java.util.Collection;

/**
 * @author noear 2025/6/20 created
 */
@Slf4j
public class ActorStateFlowTest {
    @Test
    public void case1() {
        StatefulFlowEngine flowEngine = new StatefulFlowEngineDefault(StatefulSimpleFlowDriver.builder()
                .stateController(new ActorStateController("role"))
                .stateRepository(new InMemoryStateRepository(){
                    @Override
                    public void putState(FlowContext context, Node node, StateType state) {
                        super.putState(context, node, state);
                        log.info("{} {} 完成", node.getId(), node.getTitle());
                    }
                })
                .build());

        flowEngine.load("classpath:flow/stateful/*.yml");

        /// ////////////

        FlowContext context;
        StatefulNode statefulNode;
        String instanceId = Utils.uuid();
        int amount = 900000;
        String chainId = "test1";


        context = getFlowContext(instanceId, "employee");
        statefulNode = flowEngine.getActivity(chainId, context);
        Assertions.assertEquals("n0", statefulNode.getNode().getId());
        Assertions.assertEquals(StateType.WAITING, statefulNode.getState());
        context = getFlowContext(instanceId, "employee");
        flowEngine.postOperation(context, statefulNode.getNode(), StateOperation.FORWARD);


        context = getFlowContext(instanceId, "tl");
        statefulNode = flowEngine.getActivity(chainId, context);
        Assertions.assertEquals("n1", statefulNode.getNode().getId());
        Assertions.assertEquals(StateType.WAITING, statefulNode.getState());

        context = getFlowContext(instanceId, "tl");
        flowEngine.postOperation(context, statefulNode.getNode(), StateOperation.FORWARD);


        context = getFlowContext(instanceId, "dm");
        Collection<StatefulNode> statefulNodes = flowEngine.getActivitys(chainId, context);
        for (StatefulNode auditNode : statefulNodes) {
            context = new FlowContext(instanceId).put("role", "dm"); //使用实例id
            context.put("amount", amount);
            flowEngine.postOperation(context, auditNode.getNode(), StateOperation.FORWARD);
        }

        context = getFlowContext(instanceId, "oa");
        statefulNode = flowEngine.getActivity(chainId, context);
        Assertions.assertNull(statefulNode, "必须为End节点");

    }

    private FlowContext getFlowContext(String id, String role) {
        int amount = 900000;
        return new FlowContext(id).put("role", role).put("amount", amount);
    }

    private Collection<StatefulNode> getEmailNode(StatefulFlowEngine flowEngine, String chainId, String instanceId) {
        FlowContext flowContext = getFlowContext(instanceId, "oa");
        return flowEngine.getActivitys(chainId, flowContext);
    }
}
