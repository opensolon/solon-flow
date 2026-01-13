package demo.workflow;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.FlowEngine;
import org.noear.solon.flow.Graph;

/**
 *
 * @author noear 2026/1/13 created
 *
 */
public class StopAndResumeDemo {
    @Test
    public void csae1() {
        //构建测试图
        Graph graph = Graph.create("g1", spec -> {
            spec.addStart("n1").linkAdd("n2");

            spec.addActivity("n2").task((ctx, n) -> {
                System.out.println(n.getId());
            }).linkAdd("n3");

            spec.addActivity("n3").task((ctx, n) -> {
                if (ctx.getOrDefault("paas", false) == false) {
                    ctx.stop();
                    System.out.println(n.getId() + " stop");
                } else {
                    System.out.println(n.getId() + " pass");
                }
            }).linkAdd("n4");

            spec.addEnd("n4");
        });

        FlowEngine flowEngine = FlowEngine.newInstance();
        FlowContext context = FlowContext.of("c1");

        flowEngine.eval(graph, context);

        //1.因为条件不满足，流程被中断了
        Assertions.assertTrue(context.isStopped());
        Assertions.assertFalse(context.lastRecord().isEnd()); //还没到最后结点

        //保存当前状态（存入数据库）
        String snapshotJson = context.toJson();

        //2.几天之后。。。从数据库中取出状态（条件有变了）
        context = FlowContext.fromJson(snapshotJson);
        context.put("paas", true);
        flowEngine.eval(graph, context);

        Assertions.assertFalse(context.isStopped()); //没有停止
        Assertions.assertTrue(context.lastRecord().isEnd()); //到最后结点了
    }
}
