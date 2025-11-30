package features.flow.stateful;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.noear.solon.flow.*;
import org.noear.solon.flow.stateful.StateType;
import org.noear.solon.flow.stateful.StatefulTask;
import org.noear.solon.flow.stateful.controller.NotBlockStateController;
import org.noear.solon.flow.stateful.repository.InMemoryStateRepository;

@Slf4j
public class NotBlockStateFlowTest3 {

    @Data
    @AllArgsConstructor
    public class OrderState {
        String orderId;
        String draftContent;
        boolean isApproved;
        String finalMessage;
    }

    static TaskComponent draft = (ctx, n) -> {
        OrderState state = ctx.getAs("state");

        String draft = "订单 " + state.getOrderId() + " 的销售合同草稿已生成，总价 $10000，请审核。";
        System.out.println("Node 1: [生成草稿] 完成，内容: " + draft);
        state.setDraftContent(draft);
    };

    static TaskComponent review = (ctx, n) -> {
        OrderState state = ctx.getAs("state");

        if (!state.isApproved()) {
            System.out.println("Node 2: [人工审核] 订单草稿需要人工确认。");
            throw new FlowException("需要人工审核订单 " + state.getOrderId());
        } else {
            System.out.println("Node 2: [人工审核] 已通过外部系统确认，继续流程。");
        }
    };

    static TaskComponent confirm = (ctx, n) -> {
        OrderState state = ctx.getAs("state");
        String finalMsg = "订单 " + state.getOrderId() + " 流程已完成，合同已发送。";
        System.out.println("Node 3: [最终确认] " + finalMsg);
        state.setFinalMessage(finalMsg);
    };

    FlowEngine flowEngine = FlowEngine.newInstance();
    NotBlockStateController stateController = new NotBlockStateController();
    InMemoryStateRepository stateRepository = new InMemoryStateRepository();

    @Test
    public void case1() {
        //硬编码构建流图，方便测试
        Graph graph = new GraphDecl("demo1").create(decl -> {
            decl.addNode(NodeDecl.activityOf("n1").task(draft).linkAdd("n2"));
            decl.addNode(NodeDecl.activityOf("n2").task(review).linkAdd("n3"));
            decl.addNode(NodeDecl.activityOf("n3").task(confirm));
        });

        /// ////////////
        OrderState initialState = new OrderState("o-1", null, false, null);

        FlowContext context = FlowContext.of(initialState.orderId, stateController, stateRepository)
                .put("state", initialState);

        StatefulTask task = null;
        Throwable error = null;

        try {
            task = flowEngine.forStateful().eval(graph, context);
        } catch (Exception ex) {
            error = ex;
            ex.printStackTrace();
        }

        Assertions.assertNotNull(error);
        Assertions.assertTrue(error instanceof RuntimeException);
        Assertions.assertTrue(error.getMessage().contains("需要人工审核订单"));

        //模拟人工审核后
        initialState.setApproved(true);

        try {
            //再次执行
            error = null;
            task = flowEngine.forStateful().eval(graph, context);
        } catch (Exception ex) {
            error = ex;
            ex.printStackTrace();
        }

        Assertions.assertNull(error);

        assert task.getState() == StateType.COMPLETED;
        Assertions.assertEquals("n3", task.getNode().getId());
    }
}