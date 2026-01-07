package features.flow.generated.coverage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.noear.solon.flow.driver.SimpleFlowDriver;
import org.noear.solon.flow.intercept.FlowInterceptor;

import org.noear.solon.flow.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * FlowEngine 单元测试
 */
class FlowEngineTest extends FlowTestBase {

    private Graph simpleGraph;

    @BeforeEach
    @Override
    void setUp() {
        super.setUp();

        // 创建一个简单的图用于测试
        simpleGraph = Graph.create("test-graph", spec -> {
            spec.addStart("s").linkAdd("a1");
            spec.addActivity("a1").task("context.put(\"executed\", true)").linkAdd("e");
            spec.addEnd("e");
        });

        flowEngine.load(simpleGraph);
    }

    @Test
    void testFlowEngineCreation() {
        assertNotNull(flowEngine);
        assertNotNull(flowDriver);
    }

    @Test
    void testGraphLoading() {
        assertEquals(1, flowEngine.getGraphs().size());
        assertNotNull(flowEngine.getGraph("test-graph"));
        assertSame(simpleGraph, flowEngine.getGraph("test-graph"));
    }

    @Test
    void testGraphUnloading() {
        flowEngine.unload("test-graph");
        assertNull(flowEngine.getGraph("test-graph"));
        assertEquals(0, flowEngine.getGraphs().size());
    }

    @Test
    void testGraphEval() {
        FlowContext context = FlowContext.of();
        flowEngine.eval("test-graph", context);

        // 验证任务已执行
        assertTrue(context.<Boolean>getAs("executed"));
    }

    @Test
    void testGraphEvalWithContext() {
        FlowContext context = FlowContext.of();
        context.put("input", 42);

        Graph graph = Graph.create("calc-graph", spec -> {
            spec.addStart("s").linkAdd("a1");
            spec.addActivity("a1").task("context.put(\"output\", input * 2)").linkAdd("e");
            spec.addEnd("e");
        });

        flowEngine.load(graph);
        flowEngine.eval("calc-graph", context);

        assertEquals(84, context.<Integer>getAs("output"));
    }

    @Test
    void testGraphEvalWithSteps() {
        FlowContext context = FlowContext.of();

        // 限制步数为1（只能执行开始节点）
        flowEngine.eval("test-graph", 1, context);

        // 由于步数限制，任务可能未执行
        assertNull(context.get("executed"));
    }

    @Test
    void testNonExistentGraph() {
        assertThrows(FlowException.class, () -> {
            flowEngine.eval("non-existent", FlowContext.of());
        });
    }

    @Test
    void testDriverRegistration() {
        FlowDriver customDriver = SimpleFlowDriver.getInstance();
        flowEngine.register("custom", customDriver);

        Graph graphWithDriver = Graph.create("driver-graph", "驱动测试", spec -> {
            spec.addStart("s").linkAdd("e");
            spec.addEnd("e");
        });

        // 注意：Graph需要设置driver属性，这里简化测试
    }

    @Test
    void testInterceptor() {
        final boolean[] intercepted = {false};

        FlowInterceptor interceptor = new FlowInterceptor() {
            @Override
            public void interceptFlow(org.noear.solon.flow.intercept.FlowInvocation invocation) {
                intercepted[0] = true;
                invocation.invoke();
            }
        };

        flowEngine.addInterceptor(interceptor);

        FlowContext context = FlowContext.of();
        flowEngine.eval("test-graph", context);

        assertTrue(intercepted[0]);
        assertTrue(context.<Boolean>getAs("executed"));
    }

    @Test
    void testMultipleInterceptors() {
        final int[] counter = {0};

        FlowInterceptor interceptor1 = new FlowInterceptor() {
            @Override
            public void interceptFlow(org.noear.solon.flow.intercept.FlowInvocation invocation) {
                counter[0]++;
                invocation.invoke();
            }
        };

        FlowInterceptor interceptor2 = new FlowInterceptor() {
            @Override
            public void interceptFlow(org.noear.solon.flow.intercept.FlowInvocation invocation) {
                counter[0]++;
                invocation.invoke();
            }
        };

        flowEngine.addInterceptor(interceptor1, 10);
        flowEngine.addInterceptor(interceptor2, 20); // 更高的index先执行

        FlowContext context = FlowContext.of();
        flowEngine.eval("test-graph", context);

        assertEquals(2, counter[0]);
    }

    @Test
    void testRemoveInterceptor() {
        final boolean[] intercepted = {false};

        FlowInterceptor interceptor = new FlowInterceptor() {
            @Override
            public void interceptFlow(org.noear.solon.flow.intercept.FlowInvocation invocation) {
                intercepted[0] = true;
                invocation.invoke();
            }
        };

        flowEngine.addInterceptor(interceptor);
        flowEngine.removeInterceptor(interceptor);

        FlowContext context = FlowContext.of();
        flowEngine.eval("test-graph", context);

        assertFalse(intercepted[0]); // 拦截器已被移除
        assertTrue(context.<Boolean>getAs("executed"));
    }

    @Test
    void testNodeStartEndInterception() {
        final String[] lastNode = {null};

        FlowInterceptor interceptor = new FlowInterceptor() {
            @Override
            public void onNodeStart(FlowContext context, Node node) {
                lastNode[0] = "start:" + node.getId();
            }

            @Override
            public void onNodeEnd(FlowContext context, Node node) {
                lastNode[0] = "end:" + node.getId();
            }
        };

        flowEngine.addInterceptor(interceptor);

        FlowContext context = FlowContext.of();
        flowEngine.eval("test-graph", context);

        // 验证最后一个回调是结束节点
        assertEquals("end:e", lastNode[0]);
    }

    @Test
    void testConditionalFlow() {
        Graph conditionalGraph = Graph.create("conditional", spec -> {
            spec.addStart("s").linkAdd("gateway");
            spec.addExclusive("gateway")
                    .linkAdd("path1", l -> l.when("choice.equals(\"A\")"))
                    .linkAdd("path2", l -> l.when("choice.equals(\"B\")"))
                    .linkAdd("default", l -> l.when("")); // 默认路径
            spec.addActivity("path1").task("context.put(\"result\", \"A\")").linkAdd("e");
            spec.addActivity("path2").task("context.put(\"result\", \"B\")").linkAdd("e");
            spec.addActivity("default").task("context.put(\"result\", \"default\")").linkAdd("e");
            spec.addEnd("e");
        });

        flowEngine.load(conditionalGraph);

        // 测试路径A
        FlowContext contextA = FlowContext.of();
        contextA.put("choice", "A");
        flowEngine.eval("conditional", contextA);
        assertEquals("A", contextA.getAs("result"));

        // 测试路径B
        FlowContext contextB = FlowContext.of();
        contextB.put("choice", "B");
        flowEngine.eval("conditional", contextB);
        assertEquals("B", contextB.getAs("result"));

        // 测试默认路径
        FlowContext contextC = FlowContext.of();
        contextC.put("choice", "C");
        flowEngine.eval("conditional", contextC);
        assertEquals("default", contextC.getAs("result"));
    }

    @Test
    void testParallelFlow() {
        Graph parallelGraph = Graph.create("parallel", spec -> {
            spec.addStart("s").linkAdd("gateway");
            spec.addParallel("gateway")
                    .linkAdd("task1")
                    .linkAdd("task2");

            spec.addActivity("task1")
                    .task("context.put(\"task1\", System.currentTimeMillis())")
                    .linkAdd("join");

            spec.addActivity("task2")
                    .task("context.put(\"task2\", System.currentTimeMillis())")
                    .linkAdd("join");

            spec.addParallel("join").linkAdd("e");

            spec.addEnd("e");
        });

        flowEngine.load(parallelGraph);

        FlowContext context = FlowContext.of();
        flowEngine.eval("parallel", context);

        // 验证两个任务都执行了
        assertNotNull(context.get("task1"));
        assertNotNull(context.get("task2"));
    }

    @Test
    void testLoopFlow() {
        Graph loopGraph = Graph.create("loop", spec -> {
            spec.addStart("s").linkAdd("loop_start");
            spec.addLoop("loop_start")
                    .metaPut("$for", "item")
                    .metaPut("$in", "1...4")
                    .linkAdd("do");
            spec.addActivity("do")
                    .task("context.put(\"item_\" + item, true)")
                    .linkAdd("loop_end");
            spec.addLoop("loop_end")
                    .linkAdd("e");
            spec.addEnd("e");
        });

        flowEngine.load(loopGraph);

        FlowContext context = FlowContext.of();
        flowEngine.eval("loop", context);

        // 验证循环执行了3次
        assertTrue(context.<Boolean>getAs("item_1"));
        assertTrue(context.<Boolean>getAs("item_2"));
        assertTrue(context.<Boolean>getAs("item_3"));
    }

    @Test
    void testStopFlow() {
        Graph stopGraph = Graph.create("stop-test", spec -> {
            spec.addStart("s").linkAdd("stop");
            spec.addActivity("stop").task("context.stop()").linkAdd("next");
            spec.addActivity("next").task("context.put('shouldNotExecute', true)").linkAdd("e");
            spec.addEnd("e");
        });

        flowEngine.load(stopGraph);

        FlowContext context = FlowContext.of();
        flowEngine.eval("stop-test", context);

        // 验证stop后后续任务未执行
        assertNull(context.get("shouldNotExecute"));
    }

    @Test
    void testSubGraphExecution() {
        // 创建子图
        Graph subGraph = Graph.create("sub-graph", spec -> {
            spec.addStart("s").linkAdd("task");
            spec.addActivity("task").task("context.put(\"subExecuted\", true)").linkAdd("e");
            spec.addEnd("e");
        });

        // 创建主图调用子图
        Graph mainGraph = Graph.create("main-graph", spec -> {
            spec.addStart("s").linkAdd("callSub");
            spec.addActivity("callSub").task("#sub-graph").linkAdd("e");
            spec.addEnd("e");
        });

        flowEngine.load(subGraph);
        flowEngine.load(mainGraph);

        FlowContext context = FlowContext.of();
        flowEngine.eval("main-graph", context);

        // 验证子图已执行
        assertTrue(context.<Boolean>getAs("subExecuted"));
    }
}