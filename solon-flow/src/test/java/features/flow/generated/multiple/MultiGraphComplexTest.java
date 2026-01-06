package features.flow.generated.multiple;

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
import java.util.concurrent.atomic.AtomicBoolean;
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

    private FlowEngine flowEngine;
    private MapContainer container;
    private Map<String, AtomicInteger> executionCounts;
    private Map<String, List<String>> executionPaths;

    @BeforeEach
    public void setUp() {
        container = new MapContainer();
        flowEngine = FlowEngine.newInstance(
                SimpleFlowDriver.builder()
                        .container(container)
                        .build()
        );

        executionCounts = new ConcurrentHashMap<>();
        executionPaths = new ConcurrentHashMap<>();

        // 注册一些测试用的组件
        registerTestComponents();

        // 加载多个测试图
        loadTestGraphs();
    }

    private void registerTestComponents() {
        // 计数器组件
        TaskComponent counterComponent = (context, node) -> {
            String counterName = node.getMetaAsString("counter");
            if (counterName != null) {
                executionCounts.computeIfAbsent(counterName, k -> new AtomicInteger(0))
                        .incrementAndGet();
            }
        };

        // 路径记录组件
        TaskComponent pathRecorderComponent = (context, node) -> {
            String pathKey = context.getAs("pathKey");
            if (pathKey != null) {
                executionPaths.computeIfAbsent(pathKey, k -> new ArrayList<>())
                        .add(node.getId());
            }
        };

        // 条件检查组件
        ConditionComponent thresholdCondition = (context) -> {
            Integer value = context.getAs("thresholdValue");
            Integer threshold = context.getAs("threshold");
            return value != null && threshold != null && value >= threshold;
        };

        // 停止检查组件
        ConditionComponent shouldStopCondition = (context) -> {
            Boolean shouldStop = context.getAs("shouldStop");
            return shouldStop != null && shouldStop;
        };

        // 中断检查组件
        ConditionComponent shouldInterruptCondition = (context) -> {
            Boolean shouldInterrupt = context.getAs("shouldInterrupt");
            return shouldInterrupt != null && shouldInterrupt;
        };

        container.putComponent("counter", counterComponent);
        container.putComponent("pathRecorder", pathRecorderComponent);
        container.putComponent("thresholdCondition", thresholdCondition);
        container.putComponent("shouldStopCondition", shouldStopCondition);
        container.putComponent("shouldInterruptCondition", shouldInterruptCondition);
    }

    private void loadTestGraphs() {
        // 图1：主流程（包含多个子图调用）
        Graph mainGraph = Graph.create("mainGraph", "主流程", spec -> {
            spec.addStart("start").title("开始")
                    .linkAdd("validateInput");

            spec.addActivity("validateInput").title("验证输入")
                    .task((context, node) -> {
                        String input = context.getAs("input");
                        if (input == null || input.isEmpty()) {
                            context.put("validationError", "输入不能为空");
                        }
                    })
                    .linkAdd("decision1");

            spec.addExclusive("decision1").title("决策点1")
                    .when((ConditionComponent) context ->
                            context.containsKey("validationError"))
                    .linkAdd("handleError")
                    .when((ConditionComponent) context ->
                            context.getAs("skipSubProcess") != null &&
                                    context.getAs("skipSubProcess").equals(true))
                    .linkAdd("parallelProcess")
                    .linkAdd("callSubGraph1"); // 默认分支

            spec.addActivity("handleError").title("处理错误")
                    .task((context, node) -> {
                        context.put("errorHandled", true);
                    })
                    .linkAdd("end");

            spec.addActivity("callSubGraph1").title("调用子图1")
                    .task("#subGraph1")
                    .when((ConditionComponent) context ->
                            context.getAs("stopAfterSubGraph1") != null &&
                                    context.getAs("stopAfterSubGraph1").equals(true))
                    .linkAdd("stopHere")
                    .linkAdd("parallelProcess");

            spec.addParallel("parallelProcess").title("并行处理")
                    .linkAdd("branchA")
                    .linkAdd("branchB")
                    .linkAdd("branchC");

            spec.addActivity("branchA").title("分支A")
                    .task((context, node) -> {
                        context.put("branchACompleted", true);
                    })
                    .linkAdd("mergeParallel");

            spec.addActivity("branchB").title("分支B")
                    .task("#subGraph2")
                    .linkAdd("mergeParallel");

            spec.addActivity("branchC").title("分支C")
                    .task((context, node) -> {
                        // 检查是否需要中断
                        if (context.getAs("interruptBranchC") != null &&
                                context.getAs("interruptBranchC").equals(true)) {
                            context.interrupt();
                        }
                        context.put("branchCCompleted", true);
                    })
                    .linkAdd("mergeParallel");

            spec.addActivity("mergeParallel").title("合并并行分支")
                    .task((context, node) -> {
                        context.put("parallelCompleted", true);
                    })
                    .linkAdd("callSubGraph3");

            spec.addActivity("callSubGraph3").title("调用子图3")
                    .task("#subGraph3")
                    .linkAdd("finalDecision");

            spec.addExclusive("finalDecision").title("最终决策")
                    .when("@shouldStopCondition")
                    .linkAdd("stopHere")
                    .when("@thresholdCondition")
                    .linkAdd("extraProcessing")
                    .linkAdd("end"); // 默认分支

            spec.addActivity("stopHere").title("停止点")
                    .task((context, node) -> {
                        context.stop();
                    });

            spec.addActivity("extraProcessing").title("额外处理")
                    .task("#subGraph4")
                    .linkAdd("end");

            spec.addEnd("end").title("结束");
        });

        // 子图1：数据处理子流程
        Graph subGraph1 = Graph.create("subGraph1", "数据处理子流程", spec -> {
            spec.addStart("start").title("开始")
                    .linkAdd("processData");

            spec.addActivity("processData").title("处理数据")
                    .task((context, node) -> {
                        String input = context.getAs("input");
                        if (input != null) {
                            context.put("processedData", input.toUpperCase());
                        }
                    })
                    .linkAdd("validateResult");

            spec.addActivity("validateResult").title("验证结果")
                    .task((context, node) -> {
                        String processed = context.getAs("processedData");
                        if (processed == null || processed.length() < 3) {
                            context.put("dataValidationFailed", true);
                        }
                    })
                    .linkAdd("decision");

            spec.addExclusive("decision").title("决策点")
                    .when((ConditionComponent) context ->
                            context.containsKey("dataValidationFailed"))
                    .linkAdd("handleDataError")
                    .linkAdd("finalizeData"); // 默认分支

            spec.addActivity("handleDataError").title("处理数据错误")
                    .task((context, node) -> {
                        context.put("dataErrorHandled", true);
                    });

            spec.addActivity("finalizeData").title("完成数据处理")
                    .task((context, node) -> {
                        context.put("subGraph1Completed", true);
                    })
                    .linkAdd("end");

            spec.addEnd("end").title("结束");
        });

        // 子图2：并行处理子图
        Graph subGraph2 = Graph.create("subGraph2", "并行处理子图", spec -> {
            spec.addStart("start").title("开始")
                    .linkAdd("inclusiveGateway");

            spec.addInclusive("inclusiveGateway").title("包容网关")
                    .when((ConditionComponent) context ->
                            context.getAs("enablePath1") != null &&
                                    context.getAs("enablePath1").equals(true))
                    .linkAdd("path1")
                    .when((ConditionComponent) context ->
                            context.getAs("enablePath2") != null &&
                                    context.getAs("enablePath2").equals(true))
                    .linkAdd("path2")
                    .linkAdd("defaultPath"); // 默认分支

            spec.addActivity("path1").title("路径1")
                    .task("@counter")
                    .metaPut("counter", "subGraph2_path1")
                    .linkAdd("mergeInclusive");

            spec.addActivity("path2").title("路径2")
                    .task("@counter")
                    .metaPut("counter", "subGraph2_path2")
                    .when((ConditionComponent) context -> {
                        // 模拟条件失败
                        return context.getAs("failPath2") == null ||
                                !context.getAs("failPath2").equals(true);
                    })
                    .linkAdd("mergeInclusive");

            spec.addActivity("defaultPath").title("默认路径")
                    .task("@counter")
                    .metaPut("counter", "subGraph2_default")
                    .linkAdd("mergeInclusive");

            spec.addActivity("mergeInclusive").title("合并包容路径")
                    .task((context, node) -> {
                        context.put("subGraph2Completed", true);
                    })
                    .linkAdd("end");

            spec.addEnd("end").title("结束");
        });

        // 子图3：循环处理子图
        Graph subGraph3 = Graph.create("subGraph3", "循环处理子图", spec -> {
            spec.addStart("start").title("开始")
                    .linkAdd("loopGateway");

            spec.addLoop("loopGateway").title("循环网关")
                    .metaPut("$for", "item")
                    .metaPut("$in", "items")
                    .linkAdd("processItem");

            spec.addActivity("processItem").title("处理项目")
                    .task((context, node) -> {
                        Object item = context.getAs("item");
                        List<String> processed = context.computeIfAbsent("processedItems",
                                k -> new ArrayList<>());
                        processed.add(item.toString());

                        // 检查是否需要中断当前迭代
                        if (context.getAs("interruptIteration") != null &&
                                context.getAs("interruptIteration").equals(true)) {
                            context.interrupt();
                        }
                    })
                    .linkAdd("loopGateway"); // 循环回网关

            // 注意：循环网关会自动处理流出
        });

        // 子图4：嵌套子图的子图
        Graph subGraph4 = Graph.create("subGraph4", "深度嵌套子图", spec -> {
            spec.addStart("start").title("开始")
                    .linkAdd("level1");

            spec.addActivity("level1").title("第一层")
                    .task("#subGraph4_level2")
                    .linkAdd("end");

            spec.addEnd("end").title("结束");
        });

        Graph subGraph4_level2 = Graph.create("subGraph4_level2", "第二层子图", spec -> {
            spec.addStart("start").title("开始")
                    .linkAdd("level2");

            spec.addActivity("level2").title("第二层")
                    .task((context, node) -> {
                        context.put("level2Completed", true);
                        // 深度嵌套调用
                        if (context.getAs("goDeeper") != null &&
                                context.getAs("goDeeper").equals(true)) {
                            context.exchanger().runGraph(
                                    flowEngine.getGraph("subGraph4_level3")
                            );
                        }
                    })
                    .linkAdd("end");

            spec.addEnd("end").title("结束");
        });

        Graph subGraph4_level3 = Graph.create("subGraph4_level3", "第三层子图", spec -> {
            spec.addStart("start").title("开始")
                    .linkAdd("level3");

            spec.addActivity("level3").title("第三层")
                    .task((context, node) -> {
                        context.put("level3Completed", true);
                        // 模拟停止
                        if (context.getAs("stopAtLevel3") != null &&
                                context.getAs("stopAtLevel3").equals(true)) {
                            context.stop();
                        }
                    });
            // 故意不连接结束节点，测试停止功能
        });

        // 将所有图加载到引擎
        flowEngine.load(mainGraph);
        flowEngine.load(subGraph1);
        flowEngine.load(subGraph2);
        flowEngine.load(subGraph3);
        flowEngine.load(subGraph4);
        flowEngine.load(subGraph4_level2);
        flowEngine.load(subGraph4_level3);
    }

    @Test
    public void testMultiGraphNormalFlow() {
        // 测试正常的多图嵌套流程
        FlowContext context = FlowContext.of()
                .put("input", "test data")
                .put("pathKey", "normalFlow");

        flowEngine.eval("mainGraph", context);

        // 验证主流程执行完成
        assertTrue(context.containsKey("parallelCompleted"));
        assertTrue(context.getAs("subGraph1Completed"));
        assertTrue(context.getAs("subGraph2Completed"));

        // 验证执行路径
        List<String> path = executionPaths.get("normalFlow");
        assertNotNull(path);
        assertTrue(path.contains("validateInput"));
        assertTrue(path.contains("callSubGraph1"));
        assertTrue(path.contains("parallelProcess"));

        // 验证计数器
        assertTrue(executionCounts.getOrDefault("subGraph2_default", new AtomicInteger(0)).get() > 0);
    }

    @Test
    public void testStopInMainGraph() {
        // 测试在主图中停止
        FlowContext context = FlowContext.of()
                .put("input", "test")
                .put("stopAfterSubGraph1", true)
                .put("pathKey", "stopFlow");

        flowEngine.eval("mainGraph", context);

        // 验证在stopHere节点停止
        assertTrue(context.isStopped());
        assertTrue(context.containsKey("subGraph1Completed"));
        assertFalse(context.containsKey("parallelCompleted")); // 并行处理应该未执行

        List<String> path = executionPaths.get("stopFlow");
        assertNotNull(path);
        assertTrue(path.contains("callSubGraph1"));
        assertTrue(path.contains("stopHere"));
        assertFalse(path.contains("parallelProcess")); // 应该不包含并行处理
    }

    @Test
    public void testStopInNestedGraph() {
        // 测试在深层嵌套子图中停止
        FlowContext context = FlowContext.of()
                .put("input", "test")
                .put("goDeeper", true)
                .put("stopAtLevel3", true)
                .put("pathKey", "nestedStop");

        // 跳过主流程，直接测试深度嵌套
        context.put("skipSubProcess", true);

        flowEngine.eval("mainGraph", context);

        // 验证停止发生在深层嵌套
        assertTrue(context.isStopped());
        assertTrue(context.containsKey("level2Completed"));
        assertTrue(context.containsKey("level3Completed"));

        // 验证停止后没有继续执行
        assertFalse(context.containsKey("finalDecision"));
    }

    @Test
    public void testInterruptInBranch() {
        // 测试在分支中中断
        FlowContext context = FlowContext.of()
                .put("input", "test")
                .put("interruptBranchC", true)
                .put("pathKey", "interruptFlow");

        flowEngine.eval("mainGraph", context);

        // 验证中断不影响其他分支
        assertTrue(context.containsKey("branchACompleted"));
        assertTrue(context.containsKey("subGraph2Completed")); // branchB应该完成
        assertFalse(context.containsKey("branchCCompleted")); // branchC应该被中断

        // 验证中断后流程继续
        assertTrue(context.containsKey("parallelCompleted"));
        assertTrue(context.containsKey("subGraph3"));
    }

    @Test
    public void testInterruptInLoopIteration() {
        // 测试在循环迭代中中断
        FlowContext context = FlowContext.of()
                .put("input", "test")
                .put("items", Arrays.asList("item1", "item2", "item3"))
                .put("interruptIteration", true)
                .put("pathKey", "loopInterrupt");

        // 直接调用子图3测试循环中断
        flowEngine.eval("subGraph3", context);

        // 验证部分项目被处理
        List<String> processed = context.getAs("processedItems");
        assertNotNull(processed);
        assertTrue(processed.size() > 0);
        assertTrue(processed.size() <= 3); // 可能部分迭代被中断

        // 中断不应该停止整个流程
        assertFalse(context.isStopped());
    }

    @Test
    public void testInclusiveGatewayWithMultiplePaths() {
        // 测试包容网关多路径执行
        FlowContext context = FlowContext.of()
                .put("input", "test")
                .put("enablePath1", true)
                .put("enablePath2", true)
                .put("pathKey", "inclusiveTest");

        // 直接调用子图2
        flowEngine.eval("subGraph2", context);

        // 验证两个路径都执行了
        int path1Count = executionCounts.getOrDefault("subGraph2_path1", new AtomicInteger(0)).get();
        int path2Count = executionCounts.getOrDefault("subGraph2_path2", new AtomicInteger(0)).get();

        assertTrue(path1Count > 0);
        assertTrue(path2Count > 0);
        assertTrue(context.containsKey("subGraph2Completed"));
    }

    @Test
    public void testConditionalSubGraphSkip() {
        // 测试条件跳过子图
        FlowContext context = FlowContext.of()
                .put("input", "test")
                .put("skipSubProcess", true)
                .put("pathKey", "skipSubGraph");

        flowEngine.eval("mainGraph", context);

        // 验证跳过了子图1
        assertFalse(context.containsKey("subGraph1Completed"));
        assertTrue(context.containsKey("parallelCompleted"));

        List<String> path = executionPaths.get("skipSubGraph");
        assertNotNull(path);
        assertFalse(path.contains("callSubGraph1")); // 应该不包含子图1调用
        assertTrue(path.contains("parallelProcess")); // 应该直接进入并行处理
    }

    @Test
    public void testErrorHandlingAndRecovery() {
        // 测试错误处理和恢复
        FlowContext context = FlowContext.of()
                .put("input", "") // 空输入，会触发验证错误
                .put("pathKey", "errorFlow");

        flowEngine.eval("mainGraph", context);

        // 验证错误被处理
        assertTrue(context.containsKey("validationError"));
        assertTrue(context.containsKey("errorHandled"));
        assertTrue(context.containsKey("end"));

        // 验证错误分支执行了
        List<String> path = executionPaths.get("errorFlow");
        assertNotNull(path);
        assertTrue(path.contains("validateInput"));
        assertTrue(path.contains("handleError"));
        assertFalse(path.contains("callSubGraph1")); // 应该不走正常流程
    }

    @Test
    public void testParallelExecutionWithAsync() {
        // 测试并行执行（异步）
        AtomicBoolean branchAStarted = new AtomicBoolean(false);
        AtomicBoolean branchBStarted = new AtomicBoolean(false);
        AtomicBoolean branchCStarted = new AtomicBoolean(false);

        // 创建支持异步的驱动器
        FlowDriver asyncDriver = SimpleFlowDriver.builder()
                .container(container)
                .executor(java.util.concurrent.Executors.newFixedThreadPool(3))
                .build();

        FlowEngine asyncEngine = FlowEngine.newInstance(asyncDriver);
        asyncEngine.load(flowEngine.getGraph("mainGraph"));
        asyncEngine.load(flowEngine.getGraph("subGraph2"));

        // 修改branchA以记录开始时间
        TaskComponent asyncTaskA = (context, node) -> {
            branchAStarted.set(true);
            try {
                Thread.sleep(100); // 模拟耗时操作
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            context.put("branchACompleted", true);
        };

        container.putComponent("asyncTaskA", asyncTaskA);

        FlowContext context = FlowContext.of()
                .put("input", "async test")
                .put("skipSubProcess", true);

        // 修改图以使用异步任务
        Graph modifiedMainGraph = Graph.copy(flowEngine.getGraph("mainGraph"), spec -> {
            spec.getNode("branchA").task("@asyncTaskA");
        });

        asyncEngine.load(modifiedMainGraph);
        asyncEngine.eval(modifiedMainGraph.getId(), context);

        // 验证所有分支都执行了
        assertTrue(context.containsKey("branchACompleted"));
        assertTrue(context.containsKey("subGraph2Completed"));
        assertTrue(context.containsKey("parallelCompleted"));
    }

    @Test
    public void testFlowTraceAcrossMultipleGraphs() {
        // 测试跨多图的流程跟踪
        FlowContext context = FlowContext.of()
                .put("input", "trace test")
                .enableTrace(true);

        flowEngine.eval("mainGraph", context);

        // 验证跟踪信息
        FlowTrace trace = context.trace();
        assertNotNull(trace);

        // 验证主图最后节点
        NodeRecord mainRecord = trace.lastRecord("mainGraph");
        assertNotNull(mainRecord);
        assertEquals("end", mainRecord.getId());

        // 验证子图最后节点
        NodeRecord subGraph1Record = trace.lastRecord("subGraph1");
        assertNotNull(subGraph1Record);

        NodeRecord subGraph2Record = trace.lastRecord("subGraph2");
        assertNotNull(subGraph2Record);
    }

    @Test
    public void testComplexConditionalFlow() {
        // 测试复杂条件流程
        FlowContext context = FlowContext.of()
                .put("input", "complex test")
                .put("thresholdValue", 50)
                .put("threshold", 30) // thresholdCondition 会返回 true
                .put("shouldStop", false) // shouldStopCondition 会返回 false
                .put("pathKey", "complexCondition");

        flowEngine.eval("mainGraph", context);

        // 验证走了thresholdCondition分支
        assertTrue(context.containsKey("extraProcessing"));
        assertTrue(context.containsKey("end"));

        List<String> path = executionPaths.get("complexCondition");
        assertNotNull(path);
        assertTrue(path.contains("finalDecision"));
        assertTrue(path.contains("extraProcessing"));
    }

    @Test
    public void testStopWithCondition() {
        // 测试条件停止
        FlowContext context = FlowContext.of()
                .put("input", "stop test")
                .put("shouldStop", true) // shouldStopCondition 会返回 true
                .put("pathKey", "conditionalStop");

        flowEngine.eval("mainGraph", context);

        // 验证在stopHere节点停止
        assertTrue(context.isStopped());

        List<String> path = executionPaths.get("conditionalStop");
        assertNotNull(path);
        assertTrue(path.contains("finalDecision"));
        assertTrue(path.contains("stopHere"));
        assertFalse(path.contains("end")); // 应该没有到达结束节点
    }

    @Test
    public void testRecoveryFromInterruptedState() {
        // 测试从中断状态恢复
        FlowContext context = FlowContext.of()
                .put("input", "recovery test")
                .put("interruptBranchC", true);

        // 第一次执行，branchC被中断
        flowEngine.eval("mainGraph", context);

        // 验证中断状态
        assertFalse(context.containsKey("branchCCompleted"));
        assertTrue(context.containsKey("parallelCompleted")); // 其他分支应该完成

        // 重置上下文，再次执行
        context.remove("interruptBranchC");
        context.put("branchCCompleted", false);

        // 从mergeParallel节点继续
        context.trace().recordNodeId(flowEngine.getGraph("mainGraph"), "mergeParallel");

        flowEngine.eval("mainGraph", context);

        // 验证恢复后branchC正常执行
        assertTrue(context.containsKey("branchCCompleted"));
    }

    @Test
    public void testDeepNestedStopPropagation() {
        // 测试深层嵌套的停止传播
        FlowContext context = FlowContext.of()
                .put("input", "deep stop")
                .put("goDeeper", true)
                .put("stopAtLevel3", true);

        // 直接调用深度嵌套图
        flowEngine.eval("subGraph4", context);

        // 验证停止从深层传播
        assertTrue(context.isStopped());
        assertTrue(context.containsKey("level2Completed"));
        assertTrue(context.containsKey("level3Completed"));

        // 验证停止后没有执行后续节点
        NodeRecord record = context.trace().lastRecord("subGraph4");
        assertNotNull(record);
        assertEquals("level3", record.getId()); // 最后执行的节点是level3
    }

    @Test
    public void testMultipleInterruptsInDifferentBranches() {
        // 测试不同分支中的多个中断
        FlowContext context = FlowContext.of()
                .put("input", "multi interrupt")
                .put("interruptBranchC", true)
                .put("failPath2", true) // 导致subGraph2中path2条件失败
                .put("pathKey", "multiInterrupt");

        flowEngine.eval("mainGraph", context);

        // 验证branchC被中断
        assertFalse(context.containsKey("branchCCompleted"));

        // 验证subGraph2中只有path1执行
        int path1Count = executionCounts.getOrDefault("subGraph2_path1", new AtomicInteger(0)).get();
        int path2Count = executionCounts.getOrDefault("subGraph2_path2", new AtomicInteger(0)).get();

        assertTrue(path1Count > 0);
        assertEquals(0, path2Count); // path2应该未执行

        // 验证流程继续执行
        assertTrue(context.containsKey("parallelCompleted"));
        assertTrue(context.containsKey("subGraph3"));
    }

    @Test
    public void testFlowContextSerializationBetweenGraphs() {
        // 测试图之间的上下文序列化传递
        FlowContext context = FlowContext.of("test-instance-123")
                .put("input", "serialization test")
                .put("complexData", Utils.asMap("key1", "value1", "key2", 123))
                .put("listData", Utils.asList("a", "b", "c"))
                .enableTrace(true);

        // 执行主图
        flowEngine.eval("mainGraph", context);

        // 序列化上下文
        String json = context.toJson();
        assertNotNull(json);
        assertFalse(json.isEmpty());

        // 反序列化
        FlowContext restoredContext = FlowContext.fromJson(json);

        // 验证数据恢复
        assertEquals("test-instance-123", restoredContext.getInstanceId());
        assertEquals("serialization test", restoredContext.getAs("input"));
        assertNotNull(restoredContext.getAs("complexData"));
        assertNotNull(restoredContext.getAs("listData"));

        // 验证trace信息
        FlowTrace restoredTrace = restoredContext.trace();
        assertNotNull(restoredTrace);
        assertNotNull(restoredTrace.lastRecord("mainGraph"));
    }
}