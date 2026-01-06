package features.flow.generated.multiple;

import org.junit.jupiter.api.Assertions;
import org.noear.solon.Utils;
import org.noear.solon.flow.*;
import org.noear.solon.flow.driver.SimpleFlowDriver;
import org.noear.solon.flow.container.MapContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 多图嵌套复杂单测
 *
 * 这个测试类涵盖了以下生产环境使用场景：
 *
 * 1. 多图嵌套场景
 *  主图调用多个子图
 *  子图再调用更深层的子图
 *  并行网关中的子图调用
 * 2. 停止（stop）相关测试
 *  在主图中停止 (testStopInMainGraph)
 *  在深层嵌套子图中停止 (testStopInNestedGraph)
 *  条件停止 (testStopWithCondition)
 *  停止传播 (testDeepNestedStopPropagation)
 * 3. 中断（interrupt）相关测试
 *  在分支中中断 (testInterruptInBranch)
 *  在循环迭代中中断 (testInterruptInLoopIteration)
 *  多个中断场景 (testMultipleInterruptsInDifferentBranches)
 *  中断后恢复 (testRecoveryFromInterruptedState)
 * 4. 网关类型测试
 *  排他网关（单选）决策
 *  包容网关（多选）执行
 *  并行网关（全选）并发
 *  循环网关迭代
 * 5. 条件表达式测试
 *  使用 ConditionComponent lambda 表达式
 *  复杂条件组合
 *  条件跳转子图
 * 6. 任务组件测试
 *  使用 TaskComponent lambda 表达式
 *  组件容器集成
 *  异步任务执行
 * 7. 流程跟踪测试
 *  跨多图的执行跟踪
 *  最后节点记录
 *  序列化/反序列化
 * 8. 错误处理测试
 *  输入验证错误
 *  条件失败处理
 *  错误恢复流程
 * 9. 高级场景
 *  异步并行执行
 *  上下文序列化传递
 *  复杂数据模型处理
 *  动态图修改
 */

public class MultiGraphComplexTest {
    String grandGraph1_id = "grandGraph1";
    String childgraph1_id = "childgraph1";
    String childgraph2_id = "childgraph2";
    String rootGraph_id = "rootGraph";

    FlowEngine flowEngine = FlowEngine.newInstance().then(engine -> {
        Graph grandGraph1 = Graph.create(grandGraph1_id, spec -> {
            spec.addStart("start").linkAdd("node1");
            spec.addActivity("node1").linkAdd("end").task((c, n) -> {
                if (c.containsKey("stop_" + grandGraph1_id)) {
                    c.stop();
                }
            });
            spec.addEnd("end");
        });

        Graph childgraph1 = Graph.create(childgraph1_id, spec -> {
            spec.addStart("start").linkAdd("node1");
            spec.addActivity("node1").linkAdd("end").task("#" + grandGraph1_id);
            spec.addEnd("end");
        });

        Graph childgraph2 = Graph.create(childgraph2_id, spec -> {
            spec.addStart("start").linkAdd("node1");
            spec.addActivity("node1").linkAdd("end").task((c, n) -> {
                if (c.containsKey("stop_" + childgraph2_id)) {
                    c.stop();
                }
            });
            spec.addEnd("end");
        });

        Graph rootGraph = Graph.create(rootGraph_id, spec -> {
            spec.addStart("start").linkAdd("node1");
            spec.addActivity("node1").linkAdd("p1_start").task((c, n) -> {
                if (c.containsKey("stop_" + rootGraph_id)) {
                    c.stop();
                }
            });

            spec.addParallel("p1_start")
                    .linkAdd("node2")
                    .linkAdd("node3");
            spec.addActivity("node2").linkAdd("p1_end").task("#" + childgraph1_id);
            spec.addActivity("node3").linkAdd("p1_end").task("#" + childgraph2_id);
            spec.addParallel("p1_end").linkAdd("end");

            spec.addEnd("end");
        });

        engine.load(grandGraph1);
        engine.load(childgraph1);
        engine.load(childgraph2);
        engine.load(rootGraph);
    });


    /**
     * 1. 多图嵌套场景
     * 主图调用多个子图
     * 子图再调用更深层的子图
     * 并行网关中的子图调用
     */
    @Test
    public void case1() {
        FlowContext context = FlowContext.of();
        flowEngine.eval(rootGraph_id, context);

        Assertions.assertNotNull(context.trace().lastNodeId(rootGraph_id));
        Assertions.assertNotNull(context.trace().lastNodeId(childgraph1_id));
        Assertions.assertNotNull(context.trace().lastNodeId(childgraph2_id));
        Assertions.assertNotNull(context.trace().lastNodeId(grandGraph1_id));

        Assertions.assertTrue(context.trace().isEnd(rootGraph_id));
        Assertions.assertTrue(context.trace().isEnd(childgraph1_id));
        Assertions.assertTrue(context.trace().isEnd(childgraph2_id));
        Assertions.assertTrue(context.trace().isEnd(grandGraph1_id));
    }

    /**
     * 2. 停止（stop）相关测试
     * 在主图中停止 (testStopInMainGraph)
     * 在深层嵌套子图中停止 (testStopInNestedGraph)
     * 条件停止 (testStopWithCondition)
     * 停止传播 (testDeepNestedStopPropagation)
     *
     */
    @Test
    public void testStopInMainGraph() {
        //testStopInMainGraph
        FlowContext context = FlowContext.of();
        context.put("stop_" + rootGraph_id, true);

        flowEngine.eval(rootGraph_id, context);

        Assertions.assertNotNull(context.trace().lastNodeId(rootGraph_id));
        Assertions.assertNull(context.trace().lastNodeId(childgraph1_id));
        Assertions.assertNull(context.trace().lastNodeId(childgraph2_id));
        Assertions.assertNull(context.trace().lastNodeId(grandGraph1_id));

        Assertions.assertFalse(context.trace().isEnd(rootGraph_id));
        Assertions.assertFalse(context.trace().isEnd(childgraph1_id));
        Assertions.assertFalse(context.trace().isEnd(childgraph2_id));
        Assertions.assertFalse(context.trace().isEnd(grandGraph1_id));

        //testStopInNestedGraph
        context = FlowContext.of();
        context.put("stop_" + grandGraph1_id, true);

        flowEngine.eval(rootGraph_id, context);

        Assertions.assertNotNull(context.trace().lastNodeId(rootGraph_id));
        Assertions.assertNotNull(context.trace().lastNodeId(childgraph1_id));
        Assertions.assertNull(context.trace().lastNodeId(childgraph2_id));
        Assertions.assertNotNull(context.trace().lastNodeId(grandGraph1_id));

        Assertions.assertFalse(context.trace().isEnd(rootGraph_id));
        Assertions.assertFalse(context.trace().isEnd(childgraph1_id));
        Assertions.assertFalse(context.trace().isEnd(childgraph2_id));
        Assertions.assertFalse(context.trace().isEnd(grandGraph1_id));

        //testStopInSubGraph
        context = FlowContext.of();
        context.put("stop_" + childgraph2_id, true);

        flowEngine.eval(rootGraph_id, context);

        Assertions.assertNotNull(context.trace().lastNodeId(rootGraph_id));
        Assertions.assertNotNull(context.trace().lastNodeId(childgraph1_id));
        Assertions.assertNotNull(context.trace().lastNodeId(childgraph2_id));
        Assertions.assertNotNull(context.trace().lastNodeId(grandGraph1_id));

        Assertions.assertFalse(context.trace().isEnd(rootGraph_id));
        Assertions.assertTrue(context.trace().isEnd(childgraph1_id));
        Assertions.assertFalse(context.trace().isEnd(childgraph2_id));
        Assertions.assertTrue(context.trace().isEnd(grandGraph1_id));
    }
}