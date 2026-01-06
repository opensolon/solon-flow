package features.flow.generated.single;

import org.noear.solon.Utils;
import org.noear.solon.flow.*;
import org.noear.solon.flow.driver.SimpleFlowDriver;
import org.noear.solon.flow.intercept.FlowInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.noear.solon.flow.intercept.FlowInvocation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 基于 FlowEngine.eval 和 Graph.create 的复杂单测
 * 模拟各种生产用例，包括 FlowContext.stop() 和 FlowContext.interrupt()
 *
 *
 * 这个单测类包含了以下主要测试场景：
 *
 * 1. 基本线性流程 - 测试最简单的开始→任务→结束流程
 * 2. 条件分支 - 使用 ConditionComponent lambda 表达式的排他网关
 * 3. 并行网关 - 测试并行执行和线程池集成
 * 4. FlowContext.stop() - 测试流程提前终止
 * 5. FlowContext.interrupt() - 测试分支中断
 * 6. 包容网关 - 测试多条件匹配
 * 7. 循环网关 - 测试迭代处理
 * 8. 拦截器 - 测试 FlowInterceptor 的使用
 * 9. 子图调用 - 测试图之间的调用
 * 10. 元数据访问 - 测试节点和图元数据的访问
 * 11. 复杂组合 - 混合使用停止、中断和条件
 * 12. 上下文持久化 - 测试 toJson() 和 fromJson() 方法
 */
public class FlowEngineGraphTest {
    private FlowEngine flowEngine;
    private FlowContext context;
    private List<String> executionTrace;
    private AtomicInteger taskCounter;

    @BeforeEach
    public void setUp() {
        // 创建简单的流引擎
        flowEngine = FlowEngine.newInstance();

        // 创建上下文
        context = FlowContext.of("test-instance");

        // 初始化执行跟踪列表
        executionTrace = new ArrayList<>();
        taskCounter = new AtomicInteger(0);
    }

    /**
     * 测试1: 基本线性流程
     */
    @Test
    public void testLinearFlow() throws Exception {
        // 创建线性流程图: start -> task1 -> task2 -> end
        Graph graph = Graph.create("linear-test", spec -> {
            spec.addStart("s")
                    .title("开始节点")
                    .linkAdd("task1");

            spec.addActivity("task1")
                    .title("任务1")
                    .task((ctx, node) -> {
                        executionTrace.add("task1-executed");
                        ctx.put("result1", "value1");
                    })
                    .linkAdd("task2");

            spec.addActivity("task2")
                    .title("任务2")
                    .task((ctx, node) -> {
                        executionTrace.add("task2-executed");
                        ctx.put("result2", ctx.getAs("result1") + "-processed");
                    })
                    .linkAdd("end");

            spec.addEnd("end")
                    .title("结束节点");
        });

        // 加载图到引擎
        flowEngine.load(graph);

        // 执行流程
        flowEngine.eval("linear-test", context);

        // 验证执行顺序
        Assertions.assertEquals(2, executionTrace.size());
        Assertions.assertEquals("task1-executed", executionTrace.get(0));
        Assertions.assertEquals("task2-executed", executionTrace.get(1));

        // 验证上下文数据
        Assertions.assertEquals("value1", context.getAs("result1"));
        Assertions.assertEquals("value1-processed", context.getAs("result2"));

        // 验证最后节点
        Assertions.assertEquals("end", context.lastNodeId());
    }

    /**
     * 测试2: 使用 ConditionComponent 的条件分支
     */
    @Test
    public void testConditionalBranch() throws Exception {
        // 创建条件分支图
        Graph graph = Graph.create("conditional-test", spec -> {
            spec.addStart("s")
                    .linkAdd("decision");

            spec.addExclusive("decision")
                    .title("决策节点")
                    .linkAdd("highPriority", link -> link
                            .priority(10)
                            .when((ConditionComponent) ctx ->
                                    "high".equals(ctx.getAs("priority"))))
                    .linkAdd("normalPriority", link -> link
                            .priority(5)
                            .when((ConditionComponent) ctx ->
                                    "normal".equals(ctx.getAs("priority"))))
                    .linkAdd("lowPriority", link -> link
                            .priority(1)
                            .when((ConditionComponent) ctx ->
                                    "low".equals(ctx.getAs("priority"))))
                    .linkAdd("default", link -> link
                            .priority(0));

            spec.addActivity("highPriority")
                    .task((ctx, node) -> executionTrace.add("high-priority-handled"))
                    .linkAdd("end");

            spec.addActivity("normalPriority")
                    .task((ctx, node) -> executionTrace.add("normal-priority-handled"))
                    .linkAdd("end");

            spec.addActivity("lowPriority")
                    .task((ctx, node) -> executionTrace.add("low-priority-handled"))
                    .linkAdd("end");

            spec.addActivity("default")
                    .task((ctx, node) -> executionTrace.add("default-handled"))
                    .linkAdd("end");

            spec.addEnd("end");
        });

        flowEngine.load(graph);

        // 测试高优先级分支
        context.put("priority", "high");
        flowEngine.eval("conditional-test", context);
        Assertions.assertEquals(1, executionTrace.size());
        Assertions.assertEquals("high-priority-handled", executionTrace.get(0));

        // 重置并测试正常优先级
        executionTrace.clear();
        context = FlowContext.of("test-instance-2");
        context.put("priority", "normal");
        flowEngine.eval("conditional-test", context);
        Assertions.assertEquals("normal-priority-handled", executionTrace.get(0));

        // 重置并测试默认分支（无匹配条件）
        executionTrace.clear();
        context = FlowContext.of("test-instance-3");
        context.put("priority", "unknown");
        flowEngine.eval("conditional-test", context);
        Assertions.assertEquals("default-handled", executionTrace.get(0));
    }

    /**
     * 测试3: 并行网关执行
     */
    @Test
    public void testParallelGateway() throws Exception {
        // 创建并行流程图
        Graph graph = Graph.create("parallel-test", spec -> {
            spec.addStart("s")
                    .linkAdd("parallelStart");

            spec.addParallel("parallelStart")
                    .title("并行开始")
                    .linkAdd("taskA")
                    .linkAdd("taskB")
                    .linkAdd("taskC");

            spec.addActivity("taskA")
                    .task((ctx, node) -> {
                        executionTrace.add("taskA-start");
                        try { Thread.sleep(50); } catch (InterruptedException ignored) {}
                        executionTrace.add("taskA-end");
                    })
                    .linkAdd("parallelEnd");

            spec.addActivity("taskB")
                    .task((ctx, node) -> {
                        executionTrace.add("taskB-start");
                        try { Thread.sleep(30); } catch (InterruptedException ignored) {}
                        executionTrace.add("taskB-end");
                    })
                    .linkAdd("parallelEnd");

            spec.addActivity("taskC")
                    .task((ctx, node) -> {
                        executionTrace.add("taskC-start");
                        try { Thread.sleep(10); } catch (InterruptedException ignored) {}
                        executionTrace.add("taskC-end");
                    })
                    .linkAdd("parallelEnd");

            spec.addParallel("parallelEnd")
                    .title("并行结束")
                    .linkAdd("finalTask");

            spec.addActivity("finalTask")
                    .task((ctx, node) -> executionTrace.add("final-task"))
                    .linkAdd("end");

            spec.addEnd("end");
        });

        // 使用带线程池的驱动器
        SimpleFlowDriver driver = SimpleFlowDriver.builder()
                .executor(java.util.concurrent.Executors.newFixedThreadPool(3))
                .build();

        FlowEngine parallelEngine = FlowEngine.newInstance(driver);
        parallelEngine.load(graph);

        // 执行流程
        FlowContext parallelContext = FlowContext.of("parallel-instance");
        parallelEngine.eval("parallel-test", parallelContext);

        // 等待并行任务完成
        Thread.sleep(200);

        // 验证所有任务都执行了（并行执行，顺序不定）
        Assertions.assertEquals(7, executionTrace.size());
        Assertions.assertTrue(executionTrace.contains("taskA-start"));
        Assertions.assertTrue(executionTrace.contains("taskA-end"));
        Assertions.assertTrue(executionTrace.contains("taskB-start"));
        Assertions.assertTrue(executionTrace.contains("taskB-end"));
        Assertions.assertTrue(executionTrace.contains("taskC-start"));
        Assertions.assertTrue(executionTrace.contains("taskC-end"));
        Assertions.assertTrue(executionTrace.contains("final-task"));
    }

    /**
     * 测试4: 使用 FlowContext.stop() 提前终止流程
     */
    @Test
    public void testFlowStop() throws Exception {
        AtomicBoolean stopFlag = new AtomicBoolean(false);

        Graph graph = Graph.create("stop-test", spec -> {
            spec.addStart("s")
                    .linkAdd("task1");

            spec.addActivity("task1")
                    .task((ctx, node) -> {
                        executionTrace.add("task1-executed");
                        // 根据条件停止流程
                        if (stopFlag.get()) {
                            ctx.stop();
                        }
                    })
                    .linkAdd("task2");

            spec.addActivity("task2")
                    .task((ctx, node) -> executionTrace.add("task2-executed"))
                    .linkAdd("end");

            spec.addEnd("end");
        });

        flowEngine.load(graph);

        // 测试正常执行（不停止）
        flowEngine.eval("stop-test", context);
        Assertions.assertEquals(2, executionTrace.size());
        Assertions.assertEquals("task1-executed", executionTrace.get(0));
        Assertions.assertEquals("task2-executed", executionTrace.get(1));
        Assertions.assertEquals("end", context.lastNodeId());

        // 重置并测试提前停止
        executionTrace.clear();
        context = FlowContext.of("stop-instance");
        stopFlag.set(true);

        flowEngine.eval("stop-test", context);

        // 验证只有 task1 执行了，task2 没有执行
        Assertions.assertEquals(1, executionTrace.size());
        Assertions.assertEquals("task1-executed", executionTrace.get(0));
        Assertions.assertEquals("task1", context.lastNodeId()); // 停在 task1
        Assertions.assertTrue(context.isStopped());
    }

    /**
     * 测试5: 使用 FlowContext.interrupt() 中断当前分支
     */
    @Test
    public void testFlowInterrupt() throws Exception {
        Graph graph = Graph.create("interrupt-test", spec -> {
            spec.addStart("s")
                    .linkAdd("decision");

            spec.addExclusive("decision")
                    .linkAdd("branchA")
                    .linkAdd("branchB", link -> link.when((ConditionComponent) ctx -> true))
                    .linkAdd("branchC");

            spec.addActivity("branchA")
                    .task((ctx, node) -> {
                        executionTrace.add("branchA-start");
                        // 中断当前分支
                        ctx.interrupt();
                        executionTrace.add("branchA-after-interrupt");
                    })
                    .linkAdd("merge");

            spec.addActivity("branchB")
                    .task((ctx, node) -> {
                        executionTrace.add("branchB-executed");
                    })
                    .linkAdd("merge");

            spec.addActivity("branchC")
                    .task((ctx, node) -> {
                        executionTrace.add("branchC-executed");
                    })
                    .linkAdd("merge");

            spec.addActivity("merge")
                    .task((ctx, node) -> executionTrace.add("merge-executed"))
                    .linkAdd("end");

            spec.addEnd("end");
        });

        flowEngine.load(graph);
        flowEngine.eval("interrupt-test", context);

        // 验证：branchA 被中断，branchB 正常执行（因为条件为true），branchC 不执行
        Assertions.assertEquals(2, executionTrace.size());
        Assertions.assertFalse(executionTrace.contains("branchA-start"));
        Assertions.assertTrue(executionTrace.contains("branchB-executed"));
        Assertions.assertTrue(executionTrace.contains("merge-executed"));
        Assertions.assertFalse(executionTrace.contains("branchA-after-interrupt"));
        Assertions.assertFalse(executionTrace.contains("branchC-executed"));
    }

    /**
     * 测试6: 包容网关（多条件匹配）
     */
    @Test
    public void testInclusiveGateway() throws Exception {
        Graph graph = Graph.create("inclusive-test", spec -> {
            spec.addStart("s")
                    .linkAdd("inclusiveStart");

            spec.addInclusive("inclusiveStart")
                    .title("包容网关")
                    .linkAdd("taskA", link -> link
                            .when((ConditionComponent) ctx -> ctx.getAs("conditionA")))
                    .linkAdd("taskB", link -> link
                            .when((ConditionComponent) ctx -> ctx.getAs("conditionB")))
                    .linkAdd("taskC", link -> link
                            .when((ConditionComponent) ctx -> ctx.getAs("conditionC")));

            spec.addActivity("taskA")
                    .task((ctx, node) -> executionTrace.add("taskA"))
                    .linkAdd("inclusiveEnd");

            spec.addActivity("taskB")
                    .task((ctx, node) -> executionTrace.add("taskB"))
                    .linkAdd("inclusiveEnd");

            spec.addActivity("taskC")
                    .task((ctx, node) -> executionTrace.add("taskC"))
                    .linkAdd("inclusiveEnd");

            spec.addInclusive("inclusiveEnd")
                    .title("聚合网关")
                    .linkAdd("finalTask");

            spec.addActivity("finalTask")
                    .task((ctx, node) -> executionTrace.add("final"))
                    .linkAdd("end");

            spec.addEnd("end");
        });

        flowEngine.load(graph);

        // 测试多个条件同时满足
        context.put("conditionA", true);
        context.put("conditionB", true);
        context.put("conditionC", false);

        flowEngine.eval("inclusive-test", context);

        // 验证 taskA 和 taskB 都执行了，taskC 没有执行
        Assertions.assertEquals(3, executionTrace.size()); // taskA, taskB, final, end
        Assertions.assertTrue(executionTrace.contains("taskA"));
        Assertions.assertTrue(executionTrace.contains("taskB"));
        Assertions.assertTrue(executionTrace.contains("final"));
        Assertions.assertFalse(executionTrace.contains("taskC"));
    }

    /**
     * 测试7: 循环网关（迭代处理）
     */
    @Test
    public void testLoopGateway() throws Exception {
        List<String> items = Arrays.asList("item1", "item2", "item3", "item4");

        Graph graph = Graph.create("loop-test", spec -> {
            spec.addStart("s")
                    .linkAdd("loopStart");

            spec.addLoop("loopStart")
                    .title("循环处理")
                    .metaPut("$for", "currentItem")
                    .metaPut("$in", "items")
                    .linkAdd("do");


            spec.addActivity("do").task((ctx, node) -> {
                String item = ctx.getAs("currentItem");
                executionTrace.add("processing-" + item);
                taskCounter.incrementAndGet();
            }).linkAdd("loopEnd");

            spec.addLoop("loopEnd")
                    .task((ctx, node) -> executionTrace.add("all-items-processed"))
                    .linkAdd("end");

            spec.addEnd("end");
        });

        context.put("items", items);
        flowEngine.load(graph);
        flowEngine.eval("loop-test", context);

        // 验证所有项目都被处理了
        Assertions.assertEquals(items.size() + 1, executionTrace.size()); // 4个processing + all-items-processed + end
        Assertions.assertEquals(items.size(), taskCounter.get());

        for (String item : items) {
            Assertions.assertTrue(executionTrace.contains("processing-" + item));
        }
        Assertions.assertTrue(executionTrace.contains("all-items-processed"));
    }

    /**
     * 测试8: 拦截器测试
     */
    @Test
    public void testFlowInterceptor() throws Exception {
        List<String> interceptorTrace = new ArrayList<>();

        // 创建拦截器
        FlowInterceptor interceptor = new FlowInterceptor() {
            @Override
            public void doFlowIntercept(FlowInvocation invocation) {
                interceptorTrace.add("before-flow");
                invocation.invoke();
                interceptorTrace.add("after-flow");
            }

            @Override
            public void onNodeStart(FlowContext context, Node node) {
                interceptorTrace.add("node-start:" + node.getId());
            }

            @Override
            public void onNodeEnd(FlowContext context, Node node) {
                interceptorTrace.add("node-end:" + node.getId());
            }
        };

        // 创建引擎并添加拦截器
        FlowEngine interceptorEngine = FlowEngine.newInstance();
        interceptorEngine.addInterceptor(interceptor);

        // 创建简单流程图
        Graph graph = Graph.create("interceptor-test", spec -> {
            spec.addStart("s")
                    .linkAdd("task1");

            spec.addActivity("task1")
                    .task((ctx, node) -> executionTrace.add("task1-executed"))
                    .linkAdd("end");

            spec.addEnd("end");
        });

        interceptorEngine.load(graph);
        interceptorEngine.eval("interceptor-test", context);

        // 验证拦截器调用顺序
        Assertions.assertEquals(8, interceptorTrace.size());
        Assertions.assertEquals("before-flow", interceptorTrace.get(0));
        Assertions.assertEquals("node-start:s", interceptorTrace.get(1));
        Assertions.assertEquals("node-end:s", interceptorTrace.get(2));
        Assertions.assertEquals("node-start:task1", interceptorTrace.get(3));
        Assertions.assertEquals("node-end:task1", interceptorTrace.get(4));
        Assertions.assertEquals("after-flow", interceptorTrace.get(7));
    }

    /**
     * 测试9: 子图调用测试
     */
    @Test
    public void testSubgraphInvocation() throws Exception {
        // 创建子图
        Graph subGraph = Graph.create("subgraph", spec -> {
            spec.addStart("subStart")
                    .linkAdd("subTask");

            spec.addActivity("subTask")
                    .task((ctx, node) -> {
                        executionTrace.add("subgraph-task-executed");
                        ctx.put("subgraphResult", "processed");
                    })
                    .linkAdd("subEnd");

            spec.addEnd("subEnd");
        });

        // 创建主图（调用子图）
        Graph mainGraph = Graph.create("main-graph", spec -> {
            spec.addStart("mainStart")
                    .linkAdd("callSubgraph");

            spec.addActivity("callSubgraph")
                    .title("调用子图")
                    .task("#subgraph")  // 使用 # 前缀调用子图
                    .linkAdd("mainEnd");

            spec.addEnd("mainEnd");
        });

        // 加载两个图到引擎
        flowEngine.load(subGraph);
        flowEngine.load(mainGraph);

        // 执行主图
        flowEngine.eval("main-graph", context);

        // 验证子图被正确调用
        Assertions.assertEquals(1, executionTrace.size());
        Assertions.assertEquals("subgraph-task-executed", executionTrace.get(0));
        Assertions.assertEquals("processed", context.getAs("subgraphResult"));
    }

    /**
     * 测试10: 元数据访问测试
     */
    @Test
    public void testMetadataAccess() throws Exception {
        Graph graph = Graph.create("metadata-test", spec -> {
            spec.metaPut("globalConfig", "configValue")
                    .metaPut("nested", Utils.asMap("key", "value"));

            spec.addStart("s")
                    .metaPut("startMeta", "startValue")
                    .linkAdd("task1");

            spec.addActivity("task1")
                    .title("元数据测试任务")
                    .metaPut("retryCount", 3)
                    .metaPut("timeout", 5000)
                    .task((ctx, node) -> {
                        // 访问节点元数据
                        String startMeta = node.getPrevNodes().get(0).getMetaAsString("startMeta");
                        Integer retryCount = node.getMetaAs("retryCount");
                        Long timeout = node.getMetaAsNumber("timeout").longValue();

                        ctx.put("retrievedStartMeta", startMeta);
                        ctx.put("retrievedRetryCount", retryCount);
                        ctx.put("retrievedTimeout", timeout);

                        // 访问图元数据
                        String globalConfig = node.getGraph().getMetaAs("globalConfig");
                        String nestedValue = (String) ((Map) node.getGraph().getMeta("nested")).get("key");

                        ctx.put("globalConfig", globalConfig);
                        ctx.put("nestedValue", nestedValue);
                    })
                    .linkAdd("end");

            spec.addEnd("end");
        });

        flowEngine.load(graph);
        flowEngine.eval("metadata-test", context);

        // 验证元数据访问
        Assertions.assertEquals("startValue", context.getAs("retrievedStartMeta"));
        Assertions.assertEquals(3, context.<Integer>getAs("retrievedRetryCount"));
        Assertions.assertEquals(5000L, context.<Long>getAs("retrievedTimeout"));
        Assertions.assertEquals("configValue", context.getAs("globalConfig"));
        Assertions.assertEquals("value", context.getAs("nestedValue"));
    }

    /**
     * 测试11: 复杂组合测试（停止+中断+条件）
     */
    @Test
    public void testComplexCombination() throws Exception {
        AtomicReference<String> stopAt = new AtomicReference<>();

        Graph graph = Graph.create("complex-test", spec -> {
            spec.addStart("start")
                    .linkAdd("checkInitial");

            spec.addActivity("checkInitial")
                    .task((ctx, node) -> {
                        executionTrace.add("checkInitial");
                        if ("stopAtInitial".equals(stopAt.get())) {
                            ctx.stop();
                        }
                    })
                    .linkAdd("decision");

            spec.addExclusive("decision")
                    .linkAdd("pathA", link -> link
                            .when((ConditionComponent) ctx -> "pathA".equals(ctx.getAs("path"))))
                    .linkAdd("pathB", link -> link
                            .when((ConditionComponent) ctx -> "pathB".equals(ctx.getAs("path"))));

            spec.addActivity("pathA")
                    .task((ctx, node) -> {
                        executionTrace.add("pathA-start");
                        if ("interruptPathA".equals(stopAt.get())) {
                            ctx.interrupt();
                        }
                        executionTrace.add("pathA-end");
                    })
                    .linkAdd("merge");

            spec.addActivity("pathB")
                    .task((ctx, node) -> {
                        executionTrace.add("pathB-start");
                        if ("stopAtPathB".equals(stopAt.get())) {
                            ctx.stop();
                        }
                        executionTrace.add("pathB-end");
                    })
                    .linkAdd("merge");

            spec.addActivity("merge")
                    .task((ctx, node) -> executionTrace.add("merge-executed"))
                    .linkAdd("finalCheck");

            spec.addActivity("finalCheck")
                    .task((ctx, node) -> {
                        executionTrace.add("finalCheck");
                        if ("stopAtFinal".equals(stopAt.get())) {
                            ctx.stop();
                        }
                    })
                    .linkAdd("end");

            spec.addEnd("end");
        });

        flowEngine.load(graph);

        // 测试场景1: 在pathA中断
        context = FlowContext.of("complex-1");
        context.put("path", "pathA");
        stopAt.set("interruptPathA");
        flowEngine.eval("complex-test", context);

        Assertions.assertTrue(executionTrace.contains("checkInitial"));
        Assertions.assertTrue(executionTrace.contains("pathA-start"));
        Assertions.assertTrue(executionTrace.contains("pathA-end"));
        Assertions.assertFalse(executionTrace.contains("merge-executed"));
        Assertions.assertEquals("pathA", context.lastNodeId());

        // 测试场景2: 在pathB停止
        executionTrace.clear();
        context = FlowContext.of("complex-2");
        context.put("path", "pathB");
        stopAt.set("stopAtPathB");
        flowEngine.eval("complex-test", context);

        Assertions.assertTrue(executionTrace.contains("checkInitial"));
        Assertions.assertTrue(executionTrace.contains("pathB-start"));
        Assertions.assertTrue(executionTrace.contains("pathB-end"));
        Assertions.assertFalse(executionTrace.contains("merge-executed"));
        Assertions.assertEquals("pathB", context.lastNodeId());
        Assertions.assertTrue(context.isStopped());

        // 测试场景3: 正常执行完成
        executionTrace.clear();
        context = FlowContext.of("complex-3");
        context.put("path", "pathA");
        stopAt.set(null);
        flowEngine.eval("complex-test", context);

        Assertions.assertTrue(executionTrace.contains("checkInitial"));
        Assertions.assertTrue(executionTrace.contains("pathA-start"));
        Assertions.assertTrue(executionTrace.contains("pathA-end"));
        Assertions.assertTrue(executionTrace.contains("merge-executed"));
        Assertions.assertTrue(executionTrace.contains("finalCheck"));
        Assertions.assertEquals("end", context.lastNodeId());
    }

    /**
     * 测试12: 上下文持久化测试
     */
    @Test
    public void testContextPersistence() throws Exception {
        // 创建流程图
        Graph graph = Graph.create("persistence-test", spec -> {
            spec.addStart("s")
                    .linkAdd("task1");

            spec.addActivity("task1")
                    .task((ctx, node) -> {
                        ctx.put("processedData", "重要数据");
                        ctx.put("counter", 100);
                        executionTrace.add("task1-executed");
                    })
                    .linkAdd("end");

            spec.addEnd("end");
        });

        flowEngine.load(graph);

        // 执行流程
        flowEngine.eval("persistence-test", context);

        // 将上下文序列化为JSON
        String json = context.toJson();
        Assertions.assertNotNull(json);
        Assertions.assertTrue(json.contains("重要数据"));
        Assertions.assertTrue(json.contains("counter"));

        // 从JSON反序列化
        FlowContext restoredContext = FlowContext.fromJson(json);

        // 验证数据恢复
        Assertions.assertEquals("重要数据", restoredContext.getAs("processedData"));
        Assertions.assertEquals(100, restoredContext.<Integer>getAs("counter"));
        Assertions.assertEquals("test-instance", restoredContext.getInstanceId());
    }
}