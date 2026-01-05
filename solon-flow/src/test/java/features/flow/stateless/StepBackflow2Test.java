package features.flow.stateless;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.FlowEngine;
import org.noear.solon.flow.NodeRecord;
import org.noear.solon.test.SolonTest;

/**
 *
 * @author noear 2025/12/8 created
 *
 */
@SolonTest
public class StepBackflow2Test {
    private String graphId = "backflow1";
    private String graph2Id = "backflow2";

    @Test
    public void case1() throws Throwable {
        FlowEngine flowEngine = FlowEngine.newInstance();
        flowEngine.load("classpath:flow/stateless/*.yml");


        FlowContext flowContext = FlowContext.of("x1")
                .put("a", 4)
                .put("b", 5);

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

        //========= //开始进入子图

        flowEngine.eval(graphId, 1, flowContext);
        System.out.println(flowContext.toJson());
        NodeRecord lastRecord = flowContext.trace().lastRecord(graph2Id);
        System.out.println(lastRecord.getTitle());
        Assertions.assertEquals("活动节点1b", lastRecord.getTitle());

        flowEngine.eval(graphId, 1, flowContext);
        System.out.println(flowContext.toJson());
        lastRecord = flowContext.trace().lastRecord(graph2Id);
        System.out.println(lastRecord.getTitle());
        Assertions.assertEquals("排他网关1b", lastRecord.getTitle());

        flowEngine.eval(graphId, 1, flowContext);
        System.out.println(flowContext.toJson());
        lastRecord = flowContext.trace().lastRecord(graph2Id);
        System.out.println(lastRecord.getTitle());
        Assertions.assertEquals("活动节点3b", lastRecord.getTitle());

        flowEngine.eval(graphId, 1, flowContext);
        System.out.println(flowContext.toJson());
        lastRecord = flowContext.trace().lastRecord(graph2Id);
        System.out.println(lastRecord.getTitle());
        Assertions.assertEquals("排他网关2b", lastRecord.getTitle());

        flowEngine.eval(graphId, 1, flowContext);
        System.out.println(flowContext.toJson());
        lastRecord = flowContext.trace().lastRecord(graph2Id);
        System.out.println(lastRecord.getTitle());
        Assertions.assertEquals("结束b", lastRecord.getTitle());


        //===========//回到父图

        flowEngine.eval(graphId, 1, flowContext);
        System.out.println(flowContext.lastRecord().getTitle());
        Assertions.assertEquals("排他网关2", flowContext.lastRecord().getTitle());

        flowEngine.eval(graphId, 1, flowContext);
        System.out.println(flowContext.lastRecord().getTitle());
        Assertions.assertEquals("结束", flowContext.lastRecord().getTitle());
    }
}