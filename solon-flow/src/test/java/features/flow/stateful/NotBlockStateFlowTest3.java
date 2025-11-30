package features.flow.stateful;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.noear.solon.flow.*;
import org.noear.solon.flow.stateful.StateType;
import org.noear.solon.flow.stateful.StateResult;
import org.noear.solon.flow.stateful.controller.NotBlockStateController;
import org.noear.solon.flow.stateful.repository.InMemoryStateRepository;

@Slf4j
public class NotBlockStateFlowTest3 {

    @Data
    @AllArgsConstructor
    public static class OrderState {
        String orderId;
        String draftContent;
        boolean isApproved;
        String finalMessage;
    }

    public static class Draft implements TaskComponent{
        @Override
        public void run(FlowContext ctx, Node n) throws Throwable {
            OrderState state = ctx.getAs("state");

            String draft = "订单 " + state.getOrderId() + " 的销售合同草稿已生成，总价 $10000，请审核。";
            System.out.println("Node 1: [生成草稿] 完成，内容: " + draft);
            state.setDraftContent(draft);
        }
    }

    public static class Review implements TaskComponent{
        @Override
        public void run(FlowContext ctx, Node n) throws Throwable {
            OrderState state = ctx.getAs("state");

            if (!state.isApproved()) {
                System.out.println("Node 2: [人工审核] 订单草稿需要人工确认。");
                ctx.stop();
            } else {
                System.out.println("Node 2: [人工审核] 已通过外部系统确认，继续流程。");
            }
        }
    }

    public static  class Confirm implements TaskComponent{
        @Override
        public void run(FlowContext ctx, Node n) throws Throwable {
            OrderState state = ctx.getAs("state");
            String finalMsg = "订单 " + state.getOrderId() + " 流程已完成，合同已发送。";
            System.out.println("Node 3: [最终确认] " + finalMsg);
            state.setFinalMessage(finalMsg);
        }
    }

    FlowEngine flowEngine = FlowEngine.newInstance();
    NotBlockStateController stateController = new NotBlockStateController();
    InMemoryStateRepository stateRepository = new InMemoryStateRepository();

    @Test
    public void case1() {
        //硬编码构建流图，方便测试
        Graph graph = new GraphDecl("demo1").create(decl -> {
            decl.addNode(NodeDecl.activityOf("n1").task(new Draft()).linkAdd("n2"));
            decl.addNode(NodeDecl.activityOf("n2").task(new Review()).linkAdd("n3"));
            decl.addNode(NodeDecl.activityOf("n3").task(new Confirm()));
        });

        /// ////////////
        OrderState initialState = new OrderState("o-1", null, false, null);

        FlowContext context = FlowContext.of(initialState.orderId, stateController, stateRepository)
                .put("state", initialState);

        StateResult result = null;
        result = flowEngine.forStateful().eval(graph, context);

        assert result.getState() == StateType.WAITING;
        Assertions.assertEquals("n2", result.getNode().getId());

        //模拟人工审核后
        initialState.setApproved(true);
        result = flowEngine.forStateful().eval(graph, context);

        assert result.getState() == StateType.COMPLETED;
        Assertions.assertEquals("n3", result.getNode().getId());
    }
}