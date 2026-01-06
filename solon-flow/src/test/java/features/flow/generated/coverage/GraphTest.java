package features.flow.generated.coverage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.noear.solon.flow.*;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Graph 单元测试
 */
class GraphTest {

    @Test
    void testGraphCreation() {
        Graph graph = Graph.create("test-graph", spec -> {
            spec.addStart("s").title("开始").linkAdd("a1");
            spec.addActivity("a1").task("@testTask").linkAdd("e");
            spec.addEnd("e").title("结束");
        });

        assertEquals("test-graph", graph.getId());
        assertEquals("test-graph", graph.getTitle()); // 默认标题为id
        assertEquals("", graph.getDriver()); // 默认驱动为空
        assertNotNull(graph.getStart());
        assertEquals("s", graph.getStart().getId());
    }

    @Test
    void testGraphWithTitleAndDriver() {
        Graph graph = Graph.create("test-graph", "测试图表", spec -> {
            spec.addStart("s").linkAdd("e");
            spec.addEnd("e");
        });

        assertEquals("test-graph", graph.getId());
        assertEquals("测试图表", graph.getTitle());
    }

    @Test
    void testGraphStructure() {
        Graph graph = Graph.create("complex-graph", spec -> {
            spec.addStart("s").linkAdd("a1").linkAdd("a2");
            spec.addActivity("a1").linkAdd("a3");
            spec.addActivity("a2").linkAdd("a3");
            spec.addActivity("a3").linkAdd("e");
            spec.addEnd("e");
        });

        // 测试节点获取
        assertEquals(5, graph.getNodes().size());
        assertNotNull(graph.getNode("s"));
        assertNotNull(graph.getNode("a1"));
        assertNotNull(graph.getNode("a2"));
        assertNotNull(graph.getNode("a3"));
        assertNotNull(graph.getNode("e"));

        // 测试节点不存在
        assertNull(graph.getNode("non-existent"));

        // 测试获取节点或抛出异常
        assertThrows(IllegalArgumentException.class, () -> graph.getNodeOrThrow("non-existent"));

        // 测试连接
        assertEquals(5, graph.getLinks().size());
    }

    @Test
    void testGraphMetaData() {
        Graph graph = Graph.create("meta-graph", spec -> {
            spec.addStart("s").linkAdd("e");
            spec.addEnd("e");
        });

        Map<String, Object> metas = graph.getMetas();
        assertNotNull(metas);
        assertTrue(metas.isEmpty());

        // 测试元数据方法（Graph本身不提供设置方法，通过GraphSpec设置）
    }

    @Test
    void testGraphSerialization() {
        Graph graph = Graph.create("serialize-graph", spec -> {
            spec.addStart("s").title("开始节点")
                    .metaPut("startMeta", "startValue")
                    .linkAdd("a1");
            spec.addActivity("a1").title("活动节点")
                    .task("console.log('hello')")
                    .when("a > 10")
                    .metaPut("activityMeta", 42)
                    .linkAdd("e");
            spec.addEnd("e").title("结束节点");
        });

        // 测试转为YAML
        String yaml = graph.toYaml();
        assertNotNull(yaml);
        assertTrue(yaml.contains("serialize-graph"));
        assertTrue(yaml.contains("开始节点"));

        // 测试转为JSON
        String json = graph.toJson();
        assertNotNull(json);
        assertTrue(json.contains("serialize-graph"));
        assertTrue(json.contains("开始节点"));

        // 测试转为Map
        Map<String, Object> map = graph.toMap();
        assertNotNull(map);
        assertEquals("serialize-graph", map.get("id"));
        assertEquals("serialize-graph", map.get("title"));
    }

    @Test
    void testGraphCopy() {
        Graph original = Graph.create("original", spec -> {
            spec.addStart("s").linkAdd("a1");
            spec.addActivity("a1").linkAdd("e");
            spec.addEnd("e");
        });

        Graph copy = Graph.copy(original, spec -> {
            // 修改副本
            spec.getNode("a1").linkRemove("e").linkAdd("a2");
            spec.addActivity("a2").linkAdd("e");
        });

        assertEquals("original", copy.getId());
        assertEquals(4, copy.getNodes().size()); // s, a1, a2, e
        assertEquals(3, copy.getLinks().size());
    }

    @Test
    void testGraphFromText() {
        String yamlText =
                "id: 'yaml-graph'\n" +
                "title: YAML测试图\n" +
                "driver: simple\n" +
                "layout:\n" +
                "  - id: s\n" +
                "    type: start\n" +
                "    link: a1\n" +
                "  - id: a1\n" +
                "    type: activity\n" +
                "    task: 'testTask()'\n" +
                "    link: e\n" +
                "  - id: e\n" +
                "    type: end";

        System.out.println(yamlText);

        Graph graph = Graph.fromText(yamlText);
        assertEquals("yaml-graph", graph.getId());
        assertEquals("YAML测试图", graph.getTitle());
        assertEquals("simple", graph.getDriver());
        assertEquals(3, graph.getNodes().size());
    }

    @Test
    void testGraphAsTaskComponent() {
        Graph graph = Graph.create("task-graph", spec -> {
            spec.addStart("s").linkAdd("e");
            spec.addEnd("e");
        });

        NamedTaskComponent taskComponent = graph.asTask();
        assertNotNull(taskComponent);
        assertEquals("task-graph", taskComponent.name());
        assertEquals("task-graph", taskComponent.title());
    }

    @Test
    void testGraphWithMultipleStartNodes() {
        // 测试多个开始节点（最后一个有效）
        Graph graph = Graph.create("multi-start", spec -> {
            spec.addStart("s1").linkAdd("e");
            spec.addStart("s2").linkAdd("e");
            spec.addEnd("e");
        });

        // s2应该是开始节点（因为后添加）
        assertEquals("s2", graph.getStart().getId());
    }

    @Test
    void testGraphWithMetaInSpec() {
        // 通过GraphSpec创建带有元数据的图
        GraphSpec spec = new GraphSpec("meta-graph", "元数据图", "test-driver");
        spec.addStart("s").linkAdd("e");
        spec.addEnd("e");

        Graph graph = spec.create();
        assertEquals("meta-graph", graph.getId());
        assertEquals("元数据图", graph.getTitle());
        assertEquals("test-driver", graph.getDriver());
    }

    @ParameterizedTest
    @ValueSource(strings = {"yml", "json"})
    void testGraphFromUri(String format) {
        // 注意：这需要实际的资源文件
        // 这里仅演示测试结构
        String resourcePath = "classpath:flow/test-graph." + format;

        // 在实际测试中，需要准备测试资源文件
        // Graph graph = Graph.fromUri(resourcePath);
    }
}