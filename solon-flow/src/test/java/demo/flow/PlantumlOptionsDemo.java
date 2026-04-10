package demo.flow;

import org.junit.jupiter.api.Test;
import org.noear.solon.flow.*;

/**
 * PlantumlOptions 和 displayMappingFunc 演示测试
 *
 * @author noear 2026/4/10 created
 */
public class PlantumlOptionsDemo {

    @Test
    public void test_defaultOptions() {
        Graph graph = createTestGraph();

        System.out.println("=== 默认输出 ===");
        System.out.println(graph.toPlantuml());
    }

    @Test
    public void test_showIdInTitle() {
        Graph graph = createTestGraph();

        System.out.println("\n=== 显示 Graph ID ===");
        PlantumlOptions options = new PlantumlOptions().showIdInTitle(true);
        System.out.println(graph.toPlantuml(options));
    }

    @Test
    public void test_hideGatewayType() {
        Graph graph = createTestGraph();

        System.out.println("\n=== 不显示网关类型 ===");
        PlantumlOptions options = new PlantumlOptions().showGatewayType(false);
        System.out.println(graph.toPlantuml(options));
    }

    @Test
    public void test_combinedOptions() {
        Graph graph = createTestGraph();

        System.out.println("\n=== 组合选项：不显示网关类型 + 显示 ID ===");
        PlantumlOptions options = new PlantumlOptions()
                .showGatewayType(false)
                .showIdInTitle(true);
        System.out.println(graph.toPlantuml(options));
    }

    @Test
    public void test_hideLinkWhen() {
        Graph graph = createTestGraph();

        System.out.println("\n=== 隐藏连接条件 ===");
        System.out.println(graph.toPlantuml(ctx -> {
            if (ctx.isLink()) {
                return PlantumlDisplayResult.HIDDEN;
            }
            return PlantumlDisplayResult.ofDefault();
        }));
    }

    @Test
    public void test_simplifyLinkWhen() {
        Graph graph = createTestGraph();

        System.out.println("\n=== 简化连接条件显示 ===");
        System.out.println(graph.toPlantuml(ctx -> {
            if (ctx.isLink()) {
                String when = ctx.getWhen();
                if (when != null && when.length() > 10) {
                    return PlantumlDisplayResult.of(when.substring(0, 7) + "...");
                }
            }
            return PlantumlDisplayResult.ofDefault();
        }));
    }

    @Test
    public void test_processTaskAndWhen() {
        Graph graph = createTestGraph();

        System.out.println("\n=== 同时处理 task 和 when ===");
        PlantumlOptions options = new PlantumlOptions().showIdInTitle(true);
        System.out.println(graph.toPlantuml(options, ctx -> {
            if (ctx.isNode()) {
                String task = ctx.getTask();
                if (task != null && task.startsWith("@")) {
                    // 简化方法调用显示
                    int idx = task.indexOf("::");
                    if (idx > 0) {
                        return PlantumlDisplayResult.of(task.substring(idx + 2));
                    }
                }
            } else if (ctx.isLink()) {
                // 隐藏过长的条件
                String when = ctx.getWhen();
                if (when != null && when.length() > 15) {
                    return PlantumlDisplayResult.HIDDEN;
                }
            }
            return PlantumlDisplayResult.ofDefault();
        }));
    }

    @Test
    public void test_nullOptions() {
        Graph graph = createTestGraph();

        System.out.println("\n=== null options 使用默认 ===");
        // 确保传入 null 时使用默认选项
        System.out.println(graph.toPlantuml((PlantumlOptions) null));
    }

    private Graph createTestGraph() {
        return Graph.create("testGraph", "测试流程", spec -> {
            spec.addStart("s1").title("开始").linkAdd("g1");

            spec.addExclusive("g1").title("条件检查")
                    .linkAdd("a1", link -> link.title("条件A满足").when("x > 100 && y < 50"))
                    .linkAdd("a2", link -> link.title("条件B满足").when("x <= 100 || y >= 50"));

            spec.addActivity("a1").title("处理A").task("@F_Service::processMethodA").linkAdd("e1");
            spec.addActivity("a2").title("处理B").task("@F_Service::processMethodBWithLongName").linkAdd("e1");

            spec.addEnd("e1").title("结束");
        });
    }
}
