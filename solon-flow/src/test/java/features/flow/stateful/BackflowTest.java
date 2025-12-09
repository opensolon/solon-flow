package features.flow.stateful;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.FlowEngine;
import org.noear.solon.flow.stateful.Operation;
import org.noear.solon.flow.stateful.StateResult;
import org.noear.solon.flow.stateful.controller.BlockStateController;
import org.noear.solon.test.SolonTest;

/**
 *
 * @author noear 2025/12/8 created
 *
 */
@SolonTest
public class BackflowTest {
    private String graphId = "backflow";

    @Test
    public void case1() throws Throwable {
        FlowEngine flowEngine = FlowEngine.newInstance();
        flowEngine.load("classpath:flow/stateless/*.yml");

        BlockStateController stateController = new BlockStateController();

        FlowContext flowContext = FlowContext.of("x1", stateController)
                .put("a", 4)
                .put("b", 6);


        StateResult result = flowEngine.forStateful().eval(graphId, flowContext.lastNode(), flowContext);
        System.out.println(result.getNode().getTitle() + " - " + result.getState());
        Assertions.assertEquals("活动节点1", result.getNode().getTitle());

        flowEngine.forStateful().postOperation(result.getNode(), Operation.FORWARD, flowContext);

        result = flowEngine.forStateful().eval(graphId, flowContext.lastNode(), flowContext);
        System.out.println(result.getNode().getTitle() + " - " + result.getState());
        Assertions.assertEquals("活动节点3", result.getNode().getTitle());

        flowEngine.forStateful().postOperation(result.getNode(), Operation.FORWARD, flowContext);

        result = flowEngine.forStateful().eval(graphId, flowContext.lastNode(), flowContext);
        System.out.println(result.getNode().getTitle() + " - " + result.getState());
        Assertions.assertEquals("活动节点1", result.getNode().getTitle());
    }
}