package features.flow.stateless;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.FlowEngine;
import org.noear.solon.test.SolonTest;

/**
 *
 * @author noear 2025/12/8 created
 *
 */
@SolonTest
public class StepBackflowTest {
    private String graphId = "backflow1";

    @Test
    public void case1() throws Throwable {
        FlowEngine flowEngine = FlowEngine.newInstance();
        flowEngine.load("classpath:flow/stateless/*.yml");


        FlowContext flowContext = FlowContext.of("x1")
                .put("a", 4)
                .put("b", 6);

        flowEngine.eval(graphId, 1, flowContext);
        System.out.println(flowContext.lastRecord().getTitle());
        Assertions.assertEquals("活动节点1", flowContext.lastRecord().getTitle());

        flowEngine.eval(graphId, 1, flowContext);
        System.out.println(flowContext.lastRecord().getTitle());
        Assertions.assertEquals("排他网关1", flowContext.lastRecord().getTitle());

        flowEngine.eval(graphId, 1, flowContext);
        System.out.println(flowContext.lastRecord().getTitle());
        Assertions.assertEquals("活动节点3", flowContext.lastRecord().getTitle());

        //尝试持久化转换加载
        String flowContextJson = flowContext.toJson();
        flowContext = FlowContext.fromJson(flowContextJson);

        assert flowContext.lastRecord() != null;
        assert flowContext.lastRecord().getTitle().equals("活动节点3");

        flowEngine.eval(graphId, 1, flowContext);
        System.out.println(flowContext.lastRecord().getTitle());
        Assertions.assertEquals("排他网关2", flowContext.lastRecord().getTitle());

        flowEngine.eval(graphId, 1, flowContext);
        System.out.println(flowContext.lastRecord().getTitle());
        Assertions.assertEquals("活动节点1", flowContext.lastRecord().getTitle());
    }
}