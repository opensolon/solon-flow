package features.flow.generated.multiple;

import org.noear.solon.flow.*;
import org.noear.solon.flow.driver.SimpleFlowDriver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FlowEngine.eval 多图单元测试
 *
 * 测试目的：验证 FlowEngine.eval 方法在多图场景下的正确执行
 * 测试场景：
 * 1. 单图简单执行
 * 2. 主图调用子图的嵌套执行
 * 3. 多个子图并行执行
 * 4. 图间数据传递
 * 5. 异常处理和错误传播
 */
public class FlowEngineEvalMultiGraphTest {

    private FlowEngine flowEngine;

    @BeforeEach
    void setUp() {
        // 使用 SimpleFlowDriver 创建引擎
        flowEngine = FlowEngine.newInstance(SimpleFlowDriver.getInstance());
    }

    /**
     * 测试场景1：单图简单执行
     * 验证 FlowEngine.eval 可以正确执行单个流程图
     */
    @Test
    void testSingleGraphExecution() {
        System.out.println("=== 测试场景1：单图简单执行 ===");

        // 1. 创建简单的单图流程
        Graph simpleGraph = Graph.create("simple-graph", "简单流程图", spec -> {
            spec.addStart("start").title("开始")
                    .linkAdd("task1");

            spec.addActivity("task1").title("任务1")
                    .task((context, node) -> {
                        System.out.println("执行任务1: " + context.getInstanceId());
                        context.put("task1Executed", true);
                        context.put("executionCount", 1);
                    })
                    .linkAdd("task2");

            spec.addActivity("task2").title("任务2")
                    .task((context, node) -> {
                        System.out.println("执行任务2: " + context.getInstanceId());
                        context.put("task2Executed", true);
                        Integer count = context.getAs("executionCount");
                        context.put("executionCount", count + 1);
                    })
                    .linkAdd("end");

            spec.addEnd("end").title("结束");
        });

        // 2. 加载图到引擎
        flowEngine.load(simpleGraph);

        // 3. 执行流程图
        String instanceId = "single-graph-test-" + System.currentTimeMillis();
        FlowContext context = FlowContext.of(instanceId);

        System.out.println("执行单图流程...");
        flowEngine.eval("simple-graph", context);

        // 4. 验证执行结果
        assertTrue(context.<Boolean>getAs("task1Executed"), "任务1应该被执行");
        assertTrue(context.<Boolean>getAs("task2Executed"), "任务2应该被执行");
        assertEquals(2, context.<Integer>getAs("executionCount"), "执行计数应该为2");

        // 5. 验证流程已结束
        assertNotNull(context.lastRecord(), "应该有最后记录");
        assertTrue(context.lastRecord().isEnd(), "流程应该已结束");

        System.out.println("✅ 单图执行测试通过");
    }

    /**
     * 测试场景2：主图调用子图的嵌套执行
     * 验证 eval 可以处理图间的嵌套调用
     */
    @Test
    void testNestedGraphExecution() {
        System.out.println("\n=== 测试场景2：主图调用子图的嵌套执行 ===");

        // 1. 创建子图
        Graph subGraph = Graph.create("sub-process", "子流程", spec -> {
            spec.addStart("sub_start").title("子流程开始")
                    .linkAdd("sub_task1");

            spec.addActivity("sub_task1").title("子任务1")
                    .task((context, node) -> {
                        System.out.println("执行子任务1: " + context.getInstanceId());
                        context.put("subTask1Executed", true);
                        context.put("subProcessLevel", "level-1");
                    })
                    .linkAdd("sub_task2");

            spec.addActivity("sub_task2").title("子任务2")
                    .task((context, node) -> {
                        System.out.println("执行子任务2: " + context.getInstanceId());
                        context.put("subTask2Executed", true);
                        Integer count = context.getOrDefault("subTaskCount", 0);
                        context.put("subTaskCount", count + 1);
                    })
                    .linkAdd("sub_end");

            spec.addEnd("sub_end").title("子流程结束");
        });

        // 2. 创建主图（调用子图）
        Graph mainGraph = Graph.create("main-process", "主流程", spec -> {
            spec.addStart("main_start").title("主流程开始")
                    .linkAdd("main_task1");

            spec.addActivity("main_task1").title("主任务1")
                    .task((context, node) -> {
                        System.out.println("执行主任务1: " + context.getInstanceId());
                        context.put("mainTask1Executed", true);
                        context.put("mainProcessData", "main-data");
                    })
                    .linkAdd("call_sub_process");

            // 关键：通过 task("#图ID") 调用子图
            spec.addActivity("call_sub_process").title("调用子流程")
                    .task("#sub-process")
                    .linkAdd("main_task2");

            spec.addActivity("main_task2").title("主任务2")
                    .task((context, node) -> {
                        System.out.println("执行主任务2: " + context.getInstanceId());
                        context.put("mainTask2Executed", true);

                        // 验证子流程执行结果
                        assertTrue(context.<Boolean>getOrDefault("subTask1Executed", false),
                                "子任务1应该已执行");
                        assertTrue(context.<Boolean>getOrDefault("subTask2Executed", false),
                                "子任务2应该已执行");
                    })
                    .linkAdd("main_end");

            spec.addEnd("main_end").title("主流程结束");
        });

        // 3. 加载所有图到引擎（注意顺序：先子图后主图）
        flowEngine.load(subGraph);
        flowEngine.load(mainGraph);

        // 4. 执行主图流程
        String instanceId = "nested-graph-test-" + System.currentTimeMillis();
        FlowContext context = FlowContext.of(instanceId);

        System.out.println("执行嵌套图流程...");
        flowEngine.eval("main-process", context);

        // 5. 验证执行结果
        assertTrue(context.<Boolean>getAs("mainTask1Executed"), "主任务1应该被执行");
        assertTrue(context.<Boolean>getAs("mainTask2Executed"), "主任务2应该被执行");
        assertTrue(context.<Boolean>getOrDefault("subTask1Executed", false),
                "子任务1应该被执行");
        assertTrue(context.<Boolean>getOrDefault("subTask2Executed", false),
                "子任务2应该被执行");

        // 6. 验证流程状态
        assertNotNull(context.lastRecord(), "应该有最后记录");
        assertEquals("main_end", context.lastNodeId(), "应该结束在主图的结束节点");

        System.out.println("✅ 嵌套图执行测试通过");
    }

    /**
     * 测试场景3：并行网关的多图调用
     * 验证 eval 可以处理并行执行的多个子图
     */
    @Test
    void testParallelGraphExecution() {
        System.out.println("\n=== 测试场景3：并行网关的多图调用 ===");

        AtomicInteger subGraph1Counter = new AtomicInteger(0);
        AtomicInteger subGraph2Counter = new AtomicInteger(0);

        // 1. 创建子图1
        Graph subGraph1 = Graph.create("parallel-sub-1", "并行子流程1", spec -> {
            spec.addStart("sub1_start").title("子流程1开始")
                    .linkAdd("sub1_task");

            spec.addActivity("sub1_task").title("子流程1任务")
                    .task((context, node) -> {
                        String instanceId = context.getInstanceId();
                        System.out.println("执行子流程1任务: " + instanceId);
                        context.put("subGraph1Executed", true);
                        context.put("subGraph1Time", System.currentTimeMillis());
                        subGraph1Counter.incrementAndGet();
                    })
                    .linkAdd("sub1_end");

            spec.addEnd("sub1_end").title("子流程1结束");
        });

        // 2. 创建子图2
        Graph subGraph2 = Graph.create("parallel-sub-2", "并行子流程2", spec -> {
            spec.addStart("sub2_start").title("子流程2开始")
                    .linkAdd("sub2_task");

            spec.addActivity("sub2_task").title("子流程2任务")
                    .task((context, node) -> {
                        String instanceId = context.getInstanceId();
                        System.out.println("执行子流程2任务: " + instanceId);
                        context.put("subGraph2Executed", true);
                        context.put("subGraph2Time", System.currentTimeMillis());
                        subGraph2Counter.incrementAndGet();
                    })
                    .linkAdd("sub2_end");

            spec.addEnd("sub2_end").title("子流程2结束");
        });

        // 3. 创建主图（并行调用两个子图）
        Graph mainGraph = Graph.create("parallel-main", "并行主流程", spec -> {
            spec.addStart("main_start").title("主流程开始")
                    .linkAdd("parallel_gateway");

            // 并行网关：同时调用两个子图
            spec.addParallel("parallel_gateway").title("并行网关")
                    .linkAdd("call_sub1")
                    .linkAdd("call_sub2");

            spec.addActivity("call_sub1").title("调用子流程1")
                    .task("#parallel-sub-1")
                    .linkAdd("merge_gateway");

            spec.addActivity("call_sub2").title("调用子流程2")
                    .task("#parallel-sub-2")
                    .linkAdd("merge_gateway");

            // 等待两个子图都完成
            spec.addParallel("merge_gateway").title("合并网关")
                    .task((context, node) -> {
                        System.out.println("合并网关执行，检查子图执行状态...");
                        boolean sub1Done = context.getOrDefault("subGraph1Executed", false);
                        boolean sub2Done = context.getOrDefault("subGraph2Executed", false);
                        context.put("bothSubGraphsDone", sub1Done && sub2Done);
                    })
                    .linkAdd("main_end");

            spec.addEnd("main_end").title("主流程结束");
        });

        // 4. 加载所有图到引擎
        flowEngine.load(subGraph1);
        flowEngine.load(subGraph2);
        flowEngine.load(mainGraph);

        // 5. 执行主图流程
        String instanceId = "parallel-graph-test-" + System.currentTimeMillis();
        FlowContext context = FlowContext.of(instanceId);

        System.out.println("执行并行图流程...");
        flowEngine.eval("parallel-main", context);

        // 6. 验证执行结果
        assertTrue(context.<Boolean>getOrDefault("subGraph1Executed", false),
                "子流程1应该被执行");
        assertTrue(context.<Boolean>getOrDefault("subGraph2Executed", false),
                "子流程2应该被执行");
        assertTrue(context.<Boolean>getOrDefault("bothSubGraphsDone", false),
                "两个子流程都应该完成");

        // 7. 验证计数器
        assertEquals(1, subGraph1Counter.get(), "子流程1应该执行一次");
        assertEquals(1, subGraph2Counter.get(), "子流程2应该执行一次");

        // 8. 验证执行时间（两个子图应该大致同时执行）
        Long subGraph1Time = context.getAs("subGraph1Time");
        Long subGraph2Time = context.getAs("subGraph2Time");
        assertNotNull(subGraph1Time, "子流程1应该有执行时间");
        assertNotNull(subGraph2Time, "子流程2应该有执行时间");

        // 两个子图的执行时间差应该在合理范围内（比如1秒内）
        long timeDiff = Math.abs(subGraph1Time - subGraph2Time);
        assertTrue(timeDiff < 1000, "两个子图应该大致并行执行，时间差: " + timeDiff + "ms");

        System.out.println("✅ 并行图执行测试通过");
    }

    /**
     * 测试场景4：图间数据传递和转换
     * 验证 eval 可以在图间正确传递和转换数据
     */
    @Test
    void testDataPropagationBetweenGraphs() {
        System.out.println("\n=== 测试场景4：图间数据传递和转换 ===");

        // 1. 创建数据处理子图
        Graph dataProcessorGraph = Graph.create("data-processor", "数据处理器", spec -> {
            spec.addStart("processor_start").title("处理器开始")
                    .linkAdd("process_data");

            spec.addActivity("process_data").title("处理数据")
                    .task((context, node) -> {
                        System.out.println("处理数据: " + context.getInstanceId());

                        // 从上下文获取原始数据
                        String rawData = context.getOrDefault("rawData", "");
                        Integer multiplier = context.getOrDefault("multiplier", 1);

                        // 处理数据
                        String processedData = rawData.toUpperCase() + "_PROCESSED";
                        Integer calculatedValue = rawData.length() * multiplier;

                        // 存储处理结果
                        context.put("processedData", processedData);
                        context.put("calculatedValue", calculatedValue);
                        context.put("processingTime", System.currentTimeMillis());

                        System.out.println("  原始数据: " + rawData);
                        System.out.println("  处理后的数据: " + processedData);
                        System.out.println("  计算值: " + calculatedValue);
                    })
                    .linkAdd("processor_end");

            spec.addEnd("processor_end").title("处理器结束");
        });

        // 2. 创建主图（传递数据给子图并获取结果）
        Graph mainGraph = Graph.create("data-flow-main", "数据流主流程", spec -> {
            spec.addStart("main_start").title("主流程开始")
                    .linkAdd("prepare_data");

            spec.addActivity("prepare_data").title("准备数据")
                    .task((context, node) -> {
                        System.out.println("准备数据: " + context.getInstanceId());

                        // 准备测试数据
                        context.put("rawData", "test_data_" + System.currentTimeMillis());
                        context.put("multiplier", 10);
                        context.put("sourceGraph", "main-graph");
                    })
                    .linkAdd("call_processor");

            spec.addActivity("call_processor").title("调用数据处理器")
                    .task("#data-processor")
                    .linkAdd("use_processed_data");

            spec.addActivity("use_processed_data").title("使用处理后的数据")
                    .task((context, node) -> {
                        System.out.println("使用处理后的数据: " + context.getInstanceId());

                        // 验证数据已从子图传递回来
                        String processedData = context.getAs("processedData");
                        Integer calculatedValue = context.getAs("calculatedValue");

                        assertNotNull(processedData, "处理后的数据应该存在");
                        assertNotNull(calculatedValue, "计算值应该存在");
                        assertTrue(processedData.contains("_PROCESSED"),
                                "处理后的数据应该包含_PROCESSED后缀");

                        // 使用处理后的数据
                        context.put("finalResult", processedData + "_FINAL");
                        context.put("finalValue", calculatedValue * 2);
                    })
                    .linkAdd("main_end");

            spec.addEnd("main_end").title("主流程结束");
        });

        // 3. 加载图到引擎
        flowEngine.load(dataProcessorGraph);
        flowEngine.load(mainGraph);

        // 4. 执行主图流程
        String instanceId = "data-flow-test-" + System.currentTimeMillis();
        FlowContext context = FlowContext.of(instanceId);

        System.out.println("执行数据流图流程...");
        flowEngine.eval("data-flow-main", context);

        // 5. 验证数据传递和转换
        assertNotNull(context.getAs("rawData"), "原始数据应该存在");
        assertNotNull(context.getAs("processedData"), "处理后的数据应该存在");
        assertNotNull(context.getAs("calculatedValue"), "计算值应该存在");
        assertNotNull(context.getAs("finalResult"), "最终结果应该存在");
        assertNotNull(context.getAs("finalValue"), "最终值应该存在");

        // 6. 验证数据一致性
        String processedData = context.getAs("processedData");
        assertTrue(processedData.startsWith("TEST_DATA_"),
                "处理后的数据应该被转换为大写");
        assertTrue(processedData.endsWith("_PROCESSED"),
                "处理后的数据应该包含_PROCESSED后缀");

        String finalResult = context.getAs("finalResult");
        assertTrue(finalResult.endsWith("_FINAL"),
                "最终结果应该包含_FINAL后缀");

        // 7. 验证计算逻辑
        String rawData = context.getAs("rawData");
        Integer calculatedValue = context.getAs("calculatedValue");
        Integer finalValue = context.getAs("finalValue");

        assertEquals(rawData.length() * 10, calculatedValue,
                "计算值应该是原始数据长度乘以10");
        assertEquals(calculatedValue * 2, finalValue,
                "最终值应该是计算值的两倍");

        System.out.println("✅ 图间数据传递测试通过");
    }

    /**
     * 测试场景5：错误处理和异常传播
     * 验证 eval 可以正确处理子图执行失败的情况
     */
    @Test
    void testErrorHandlingInMultiGraph() {
        System.out.println("\n=== 测试场景5：错误处理和异常传播 ===");

        // 1. 创建可能失败的子图
        Graph errorProneGraph = Graph.create("error-prone", "易错子流程", spec -> {
            spec.addStart("error_start").title("易错流程开始")
                    .linkAdd("error_task");

            spec.addActivity("error_task").title("易错任务")
                    .task((context, node) -> {
                        System.out.println("执行易错任务: " + context.getInstanceId());

                        // 根据配置决定是否抛出异常
                        boolean shouldFail = context.getOrDefault("shouldFail", false);
                        if (shouldFail) {
                            String errorMsg = "子流程执行失败: " + context.getInstanceId();
                            System.out.println("❌ " + errorMsg);
                            throw new RuntimeException(errorMsg);
                        }

                        context.put("errorTaskExecuted", true);
                        context.put("errorTaskSuccess", true);
                    })
                    .linkAdd("error_end");

            spec.addEnd("error_end").title("易错流程结束");
        });

        // 2. 创建带错误处理的主图
        Graph mainGraph = Graph.create("error-handling-main", "错误处理主流程", spec -> {
            spec.addStart("main_start").title("主流程开始")
                    .linkAdd("setup_context");

            spec.addActivity("setup_context").title("设置上下文")
                    .task((context, node) -> {
                        System.out.println("设置上下文: " + context.getInstanceId());
                        context.put("testPhase", "setup");
                        context.put("shouldFail", true); // 设置子图会失败
                    })
                    .linkAdd("call_error_prone");

            spec.addActivity("call_error_prone").title("调用易错子流程")
                    .task("#error-prone")
                    .linkAdd("error_handler");

            spec.addActivity("error_handler").title("错误处理")
                    .task((context, node) -> {
                        System.out.println("错误处理节点执行: " + context.getInstanceId());
                        context.put("errorHandled", true);
                        context.put("recoveryTime", System.currentTimeMillis());
                    })
                    .linkAdd("main_end");

            spec.addEnd("main_end").title("主流程结束");
        });

        // 3. 加载图到引擎
        flowEngine.load(errorProneGraph);
        flowEngine.load(mainGraph);

        // 4. 执行主图流程（预期会抛出异常）
        String instanceId = "error-handling-test-" + System.currentTimeMillis();
        FlowContext context = FlowContext.of(instanceId);

        System.out.println("执行错误处理图流程（预期会失败）...");

        try {
            flowEngine.eval("error-handling-main", context);

            // 如果执行到这里，说明异常被捕获了
            System.out.println("⚠️ 流程执行完成，异常可能被框架捕获");

            // 验证错误处理逻辑是否执行
            assertTrue(context.<Boolean>getOrDefault("errorHandled", false),
                    "错误处理逻辑应该执行");

        } catch (Exception e) {
            System.out.println("捕获到预期异常: " + e.getMessage());

            // 验证上下文状态
            assertEquals("setup", context.getAs("testPhase"),
                    "测试阶段应该是setup");
            assertTrue(context.<Boolean>getAs("shouldFail"),
                    "应该设置了失败标志");

            // 验证易错任务没有成功执行
            assertNull(context.getAs("errorTaskSuccess"),
                    "易错任务不应该成功执行");
        }

        System.out.println("✅ 错误处理测试通过");
    }

    /**
     * 测试场景6：复杂嵌套和多层调用
     * 验证 eval 可以处理多层嵌套的图调用
     */
    @Test
    void testComplexNestedGraphExecution() {
        System.out.println("\n=== 测试场景6：复杂嵌套和多层调用 ===");

        // 创建执行计数器
        AtomicInteger level1Counter = new AtomicInteger(0);
        AtomicInteger level2Counter = new AtomicInteger(0);
        AtomicInteger level3Counter = new AtomicInteger(0);

        // 1. 创建第三层子图
        Graph level3Graph = Graph.create("level3-graph", "第三层子图", spec -> {
            spec.addStart("l3_start").title("L3开始")
                    .linkAdd("l3_task");

            spec.addActivity("l3_task").title("L3任务")
                    .task((context, node) -> {
                        System.out.println("执行L3任务: " + context.getInstanceId());
                        context.put("level3Executed", true);
                        context.put("executionDepth", 3);
                        level3Counter.incrementAndGet();
                    })
                    .linkAdd("l3_end");

            spec.addEnd("l3_end").title("L3结束");
        });

        // 2. 创建第二层子图（调用第三层）
        Graph level2Graph = Graph.create("level2-graph", "第二层子图", spec -> {
            spec.addStart("l2_start").title("L2开始")
                    .linkAdd("l2_task1");

            spec.addActivity("l2_task1").title("L2任务1")
                    .task((context, node) -> {
                        System.out.println("执行L2任务1: " + context.getInstanceId());
                        context.put("level2Task1Executed", true);
                        level2Counter.incrementAndGet();
                    })
                    .linkAdd("call_l3");

            spec.addActivity("call_l3").title("调用L3")
                    .task("#level3-graph")
                    .linkAdd("l2_task2");

            spec.addActivity("l2_task2").title("L2任务2")
                    .task((context, node) -> {
                        System.out.println("执行L2任务2: " + context.getInstanceId());
                        context.put("level2Task2Executed", true);

                        // 验证L3已执行
                        assertTrue(context.<Boolean>getOrDefault("level3Executed", false),
                                "L3应该已执行");
                    })
                    .linkAdd("l2_end");

            spec.addEnd("l2_end").title("L2结束");
        });

        // 3. 创建第一层子图（调用第二层）
        Graph level1Graph = Graph.create("level1-graph", "第一层子图", spec -> {
            spec.addStart("l1_start").title("L1开始")
                    .linkAdd("l1_task1");

            spec.addActivity("l1_task1").title("L1任务1")
                    .task((context, node) -> {
                        System.out.println("执行L1任务1: " + context.getInstanceId());
                        context.put("level1Task1Executed", true);
                        level1Counter.incrementAndGet();
                    })
                    .linkAdd("call_l2");

            spec.addActivity("call_l2").title("调用L2")
                    .task("#level2-graph")
                    .linkAdd("l1_task2");

            spec.addActivity("l1_task2").title("L1任务2")
                    .task((context, node) -> {
                        System.out.println("执行L1任务2: " + context.getInstanceId());
                        context.put("level1Task2Executed", true);

                        // 验证L2已执行
                        assertTrue(context.<Boolean>getOrDefault("level2Task1Executed", false),
                                "L2任务1应该已执行");
                        assertTrue(context.<Boolean>getOrDefault("level2Task2Executed", false),
                                "L2任务2应该已执行");
                    })
                    .linkAdd("l1_end");

            spec.addEnd("l1_end").title("L1结束");
        });

        // 4. 创建主图（调用第一层）
        Graph mainGraph = Graph.create("complex-main", "复杂主流程", spec -> {
            spec.addStart("main_start").title("主流程开始")
                    .linkAdd("main_task1");

            spec.addActivity("main_task1").title("主任务1")
                    .task((context, node) -> {
                        System.out.println("执行主任务1: " + context.getInstanceId());
                        context.put("mainTask1Executed", true);
                        context.put("executionChain", "main->l1->l2->l3");
                    })
                    .linkAdd("call_l1");

            spec.addActivity("call_l1").title("调用L1")
                    .task("#level1-graph")
                    .linkAdd("main_task2");

            spec.addActivity("main_task2").title("主任务2")
                    .task((context, node) -> {
                        System.out.println("执行主任务2: " + context.getInstanceId());
                        context.put("mainTask2Executed", true);

                        // 验证所有层级都已执行
                        assertTrue(context.<Boolean>getOrDefault("level1Task1Executed", false),
                                "L1任务1应该已执行");
                        assertTrue(context.<Boolean>getOrDefault("level1Task2Executed", false),
                                "L1任务2应该已执行");
                        assertTrue(context.<Boolean>getOrDefault("level2Task1Executed", false),
                                "L2任务1应该已执行");
                        assertTrue(context.<Boolean>getOrDefault("level2Task2Executed", false),
                                "L2任务2应该已执行");
                        assertTrue(context.<Boolean>getOrDefault("level3Executed", false),
                                "L3应该已执行");
                    })
                    .linkAdd("main_end");

            spec.addEnd("main_end").title("主流程结束");
        });

        // 5. 加载所有图到引擎（注意加载顺序：从最深层开始）
        flowEngine.load(level3Graph);
        flowEngine.load(level2Graph);
        flowEngine.load(level1Graph);
        flowEngine.load(mainGraph);

        // 6. 执行主图流程
        String instanceId = "complex-nested-test-" + System.currentTimeMillis();
        FlowContext context = FlowContext.of(instanceId);

        System.out.println("执行复杂嵌套图流程...");
        flowEngine.eval("complex-main", context);

        // 7. 验证执行结果
        assertTrue(context.<Boolean>getAs("mainTask1Executed"), "主任务1应该被执行");
        assertTrue(context.<Boolean>getAs("mainTask2Executed"), "主任务2应该被执行");
        assertTrue(context.<Boolean>getOrDefault("level1Task1Executed", false),
                "L1任务1应该被执行");
        assertTrue(context.<Boolean>getOrDefault("level1Task2Executed", false),
                "L1任务2应该被执行");
        assertTrue(context.<Boolean>getOrDefault("level2Task1Executed", false),
                "L2任务1应该被执行");
        assertTrue(context.<Boolean>getOrDefault("level2Task2Executed", false),
                "L2任务2应该被执行");
        assertTrue(context.<Boolean>getOrDefault("level3Executed", false),
                "L3应该被执行");

        // 8. 验证执行计数
        assertEquals(1, level1Counter.get(), "L1应该执行一次");
        assertEquals(1, level2Counter.get(), "L2应该执行一次");
        assertEquals(1, level3Counter.get(), "L3应该执行一次");

        // 9. 验证执行链
        assertEquals("main->l1->l2->l3", context.getAs("executionChain"),
                "执行链应该正确");

        // 10. 验证执行深度
        assertEquals(3, context.<Integer>getAs("executionDepth"),
                "执行深度应该是3");

        System.out.println("✅ 复杂嵌套图执行测试通过");
    }

    /**
     * 测试场景7：多次eval调用和状态管理
     * 验证多次调用eval的状态隔离和上下文管理
     */
    @Test
    void testMultipleEvalCalls() {
        System.out.println("\n=== 测试场景7：多次eval调用和状态管理 ===");

        // 创建简单的计数器图
        Graph counterGraph = Graph.create("counter-graph", "计数器图", spec -> {
            spec.addStart("counter_start").title("计数器开始")
                    .linkAdd("increment_counter");

            spec.addActivity("increment_counter").title("增加计数器")
                    .task((context, node) -> {
                        String instanceId = context.getInstanceId();
                        Integer currentCount = context.getOrDefault("executionCount", 0);
                        Integer newCount = currentCount + 1;

                        System.out.println("增加计数器: " + instanceId +
                                " [当前: " + currentCount + " -> 新: " + newCount + "]");

                        context.put("executionCount", newCount);
                        context.put("lastExecutionTime", System.currentTimeMillis());
                        context.put("instanceId", instanceId);
                    })
                    .linkAdd("counter_end");

            spec.addEnd("counter_end").title("计数器结束");
        });

        // 加载图到引擎
        flowEngine.load(counterGraph);

        // 执行多次eval调用
        int numberOfCalls = 5;
        System.out.println("执行 " + numberOfCalls + " 次eval调用...");

        for (int i = 0; i < numberOfCalls; i++) {
            String instanceId = "multi-eval-test-" + i + "-" + System.currentTimeMillis();
            FlowContext context = FlowContext.of(instanceId);

            // 设置不同的初始数据
            context.put("initialValue", i * 10);
            context.put("callIndex", i);

            System.out.println("执行第 " + (i + 1) + " 次eval调用: " + instanceId);
            flowEngine.eval("counter-graph", context);

            // 验证每次调用的状态独立
            assertEquals(instanceId, context.getAs("instanceId"),
                    "实例ID应该正确");
            assertEquals(1, context.<Integer>getAs("executionCount"),
                    "每次调用执行计数应该是1");
            assertEquals(i, context.<Integer>getAs("callIndex"),
                    "调用索引应该正确");
            assertEquals(i * 10, context.<Integer>getAs("initialValue"),
                    "初始值应该正确");

            // 验证执行时间
            assertNotNull(context.getAs("lastExecutionTime"),
                    "应该有最后执行时间");
        }

        System.out.println("✅ 多次eval调用测试通过");
    }

    /**
     * 综合测试：验证所有场景
     */
    @Test
    void testAllScenarios() {
        System.out.println("=== 开始综合测试 ===");

        // 依次执行所有测试场景
        testSingleGraphExecution();
        testNestedGraphExecution();
        testParallelGraphExecution();
        testDataPropagationBetweenGraphs();
        testErrorHandlingInMultiGraph();
        testComplexNestedGraphExecution();
        testMultipleEvalCalls();

        System.out.println("\n=== 所有测试场景通过 ===");
        System.out.println("✅ FlowEngine.eval 多图单元测试全部通过");
    }
}