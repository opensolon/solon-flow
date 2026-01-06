package features.flow.generated.coverage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.noear.dami2.bus.DamiBus;
import org.noear.solon.flow.*;
import org.noear.solon.flow.driver.SimpleFlowDriver;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FlowContext 单元测试
 */
class FlowContextTest {

    private FlowContextDefault context;

    @BeforeEach
    void setUp() {
        context = new FlowContextDefault("test-instance");
    }

    @Test
    void testConstructorWithInstanceId() {
        assertEquals("test-instance", context.getAs("instanceId"));
        assertEquals("test-instance", context.getInstanceId());
    }

    @Test
    void testConstructorWithoutInstanceId() {
        FlowContextDefault ctx = new FlowContextDefault();
        assertEquals("", ctx.getInstanceId());
    }

    @Test
    void testStaticFactoryMethods() {
        FlowContext ctx1 = FlowContext.of();
        assertNotNull(ctx1);
        assertTrue(ctx1 instanceof FlowContextDefault);

        FlowContext ctx2 = FlowContext.of("custom-id");
        assertEquals("custom-id", ctx2.getInstanceId());
    }

    @Test
    void testToJsonAndFromJson() {
        context.put("key1", "value1");
        context.put("key2", 123);
        context.put("key3", true);

        Map<String, Object> map = new HashMap<>();
        map.put("nestedKey", "nestedValue");
        context.put("key4", map);

        String json = context.toJson();
        assertNotNull(json);
        assertTrue(json.contains("key1"));
        assertTrue(json.contains("value1"));

        FlowContext restored = FlowContext.fromJson(json);
        assertEquals("value1", restored.getAs("key1"));
        assertEquals(123, restored.<Integer>getAs("key2"));
        assertEquals(true, restored.getAs("key3"));
    }

    @Test
    void testEventBus() {
        DamiBus bus = context.eventBus();
        assertNotNull(bus);

        DamiBus bus2 = context.eventBus();
        assertSame(bus, bus2); // 应该返回同一个实例
    }

    @Test
    void testThenMethod() {
        AtomicInteger counter = new AtomicInteger(0);
        FlowContext result = context.then(ctx -> {
            counter.incrementAndGet();
            ctx.put("thenKey", "thenValue");
        });

        assertEquals(1, counter.get());
        assertSame(context, result);
        assertEquals("thenValue", context.getAs("thenKey"));
    }

    @Test
    void testInterruptAndStop() {
        // 测试默认情况下不停止
        assertFalse(context.isStopped());

        context.stop();
        assertTrue(context.isStopped() == false);

        // 重置停止状态
        context.stopped(false);
        assertFalse(context.isStopped());
    }

    @Test
    void testTraceMethods() {
        FlowTrace trace = context.trace();
        assertNotNull(trace);

        // 测试启用/禁用跟踪
        context.enableTrace(false);
        assertFalse(trace.isEnabled());

        context.enableTrace(true);
        assertTrue(trace.isEnabled());

        // 测试最后节点相关方法
        assertNull(context.lastRecord());
        assertNull(context.lastNodeId());
    }

    @Test
    void testModelOperations() {
        int baseSize = 2;
        Map<String, Object> model = context.model();
        assertNotNull(model);
        assertEquals(baseSize, model.size()); // 默认情况下应该有两个键值对

        // 测试 put 方法
        context.put("stringKey", "stringValue");
        context.put("intKey", 42);
        context.put("nullKey", null); // null 值应该被忽略

        assertTrue(model.containsKey("stringKey"));
        assertFalse(model.containsKey("nullKey"));
        assertEquals(2 + baseSize, model.size());

        // 测试 get 方法
        assertEquals("stringValue", context.get("stringKey"));
        assertEquals(42, context.get("intKey"));

        // 测试 getAs 方法
        String strValue = context.getAs("stringKey");
        Integer intValue = context.getAs("intKey");
        assertEquals("stringValue", strValue);
        assertEquals(42, intValue.intValue());

        // 测试 getOrDefault 方法
        String defValue = context.getOrDefault("nonExistent", "default");
        assertEquals("default", defValue);

        // 测试 containsKey 方法
        assertTrue(context.containsKey("stringKey"));
        assertFalse(context.containsKey("nonExistent"));

        // 测试 remove 方法
        context.remove("stringKey");
        assertFalse(context.containsKey("stringKey"));
        assertEquals(1 + baseSize, model.size());

        // 测试 putAll 方法
        Map<String, Object> newData = new HashMap<>();
        newData.put("key1", "value1");
        newData.put("key2", "value2");
        context.putAll(newData);
        assertEquals(3 + baseSize, model.size());
        assertEquals("value1", context.get("key1"));

        // 测试 putIfAbsent 方法
        context.putIfAbsent("key1", "newValue1"); // 应该不覆盖
        context.putIfAbsent("key3", "value3"); // 应该添加
        assertEquals("value1", context.get("key1"));
        assertEquals("value3", context.get("key3"));

        // 测试 computeIfAbsent 方法
        String computed = context.computeIfAbsent("key4", k -> "computed-" + k);
        assertEquals("computed-key4", computed);
        assertEquals("computed-key4", context.get("key4"));
    }

    @Test
    void testWithMethod() throws Exception {
        assertNull(context.get("tempKey"));

        context.with("tempKey", "tempValue", () -> {
            assertEquals("tempValue", context.get("tempKey"));
        });

        assertNull(context.get("tempKey"));
    }

    @Test
    void testWithMethodRestoresOriginalValue() throws Exception {
        context.put("key", "original");

        context.with("key", "temporary", () -> {
            assertEquals("temporary", context.get("key"));
        });

        assertEquals("original", context.get("key"));
    }

    @Test
    void testWithMethodExceptionHandling() {
        context.put("key", "original");

        assertThrows(RuntimeException.class, () -> {
            context.with("key", "temporary", () -> {
                throw new RuntimeException("Test exception");
            });
        });

        // 即使抛出异常，原始值也应该被恢复
        assertEquals("original", context.get("key"));
    }

    @ParameterizedTest
    @CsvSource({
            "stringKey, Hello World",
            "intKey, 123",
            "boolKey, true",
            "doubleKey, 3.14"
    })
    void testPutAndGetVariousTypes(String key, Object value) {
        context.put(key, value);
        assertEquals(value, context.get(key));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "test"})
    void testInstanceId(String instanceId) {
        FlowContext ctx = FlowContext.of(instanceId);
        assertEquals(instanceId == null ? "" : instanceId, ctx.getInstanceId());
    }

    @Test
    void testExchangerMethods() {
        FlowContextInternal internalCtx = (FlowContextInternal) context;

        assertNull(internalCtx.exchanger());

        FlowExchanger mockExchanger = new FlowExchanger(
                FlowEngine.newInstance(),
                SimpleFlowDriver.getInstance(),
                context,
                -1,
                null
        );

        internalCtx.exchanger(mockExchanger);
        assertSame(mockExchanger, internalCtx.exchanger());
    }

    @Test
    void testJsonSerializationWithTrace() {
        context.enableTrace(true);

        Graph graph = Graph.create("test-graph", spec -> {
            spec.addStart("s").linkAdd("e");
            spec.addEnd("e");
        });

        context.trace().recordNode(graph, graph.getNode("s"));

        String json = context.toJson();
        FlowContext restored = FlowContext.fromJson(json);

        assertNotNull(restored.trace());
        assertTrue(restored.trace().isEnabled());
    }
}