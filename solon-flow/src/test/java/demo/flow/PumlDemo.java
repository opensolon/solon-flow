package demo.flow;

import org.noear.solon.flow.*;

/**
 * PlantUML 输出示例
 *
 * @author noear 2026/3/22 created
 */
public class PumlDemo {

    /**
     * 默认输出
     */
    public String case1_default(Graph graph) {
        return graph.toPlantuml();
    }

    /**
     * 仅指定选项：不显示网关类型名
     */
    public String case2_hideGatewayType(Graph graph) {
        PlantumlOptions options = new PlantumlOptions().showGatewayType(false);
        return graph.toPlantuml(options);
    }

    /**
     * 仅指定选项：在标题中显示 ID
     */
    public String case3_showIdInTitle(Graph graph) {
        PlantumlOptions options = new PlantumlOptions().showIdInTitle(true);
        return graph.toPlantuml(options);
    }

    /**
     * 仅指定选项：组合多个选项
     */
    public String case4_combinedOptions(Graph graph) {
        PlantumlOptions options = new PlantumlOptions()
                .showGatewayType(false)
                .showIdInTitle(true);
        return graph.toPlantuml(options);
    }

    /**
     * 映射函数：截断过长的 task
     */
    public String case5_truncateTask(Graph graph) {
        return graph.toPlantuml(ctx -> {
            if (ctx.isNode()) {
                String task = ctx.getTask();
                if (task != null && task.length() > 20) {
                    return PlantumlDisplayResult.of(task.substring(0, 17) + "...");
                }
            }
            return PlantumlDisplayResult.ofDefault();
        });
    }

    /**
     * 映射函数：隐藏子图调用
     */
    public String case6_hideSubgraphCall(Graph graph) {
        return graph.toPlantuml(ctx -> {
            if (ctx.isNode()) {
                String task = ctx.getTask();
                if (task != null && task.startsWith("#")) {
                    return PlantumlDisplayResult.HIDDEN;
                }
            }
            return PlantumlDisplayResult.ofDefault();
        });
    }

    /**
     * 映射函数：自定义子图调用显示
     */
    public String case7_customSubgraphDisplay(Graph graph) {
        return graph.toPlantuml(ctx -> {
            if (ctx.isNode()) {
                String task = ctx.getTask();
                if (task != null && task.startsWith("#")) {
                    return PlantumlDisplayResult.of("子图: " + task.substring(1));
                }
            }
            return PlantumlDisplayResult.ofDefault();
        });
    }

    /**
     * 映射函数：隐藏 link when 条件
     */
    public String case8_hideLinkWhen(Graph graph) {
        return graph.toPlantuml(ctx -> {
            if (ctx.isLink()) {
                // 隐藏所有连接上的条件描述
                return PlantumlDisplayResult.HIDDEN;
            }
            return PlantumlDisplayResult.ofDefault();
        });
    }

    /**
     * 映射函数：简化 link when 条件显示
     */
    public String case9_simplifyLinkWhen(Graph graph) {
        return graph.toPlantuml(ctx -> {
            if (ctx.isLink()) {
                String when = ctx.getWhen();
                if (when != null && when.length() > 15) {
                    return PlantumlDisplayResult.of(when.substring(0, 12) + "...");
                }
            }
            return PlantumlDisplayResult.ofDefault();
        });
    }

    /**
     * 映射函数：同时处理 task 和 when
     */
    public String case10_processBoth(Graph graph) {
        return graph.toPlantuml(ctx -> {
            if (ctx.isNode()) {
                String task = ctx.getTask();
                if (task != null) {
                    // 隐藏子图调用
                    if (task.startsWith("#")) {
                        return PlantumlDisplayResult.HIDDEN;
                    }
                    // 简化方法调用
                    if (task.startsWith("@")) {
                        int idx = task.indexOf("::");
                        if (idx > 0) {
                            return PlantumlDisplayResult.of(task.substring(idx + 2));
                        }
                    }
                }
            } else if (ctx.isLink()) {
                String when = ctx.getWhen();
                if (when != null && when.length() > 20) {
                    return PlantumlDisplayResult.of(when.substring(0, 17) + "...");
                }
            }
            return PlantumlDisplayResult.ofDefault();
        });
    }

    /**
     * 组合使用：选项 + 映射函数
     */
    public String case11_optionsWithMapping(Graph graph) {
        PlantumlOptions options = new PlantumlOptions()
                .showGatewayType(false)
                .showIdInTitle(true);
        return graph.toPlantuml(options, ctx -> {
            if (ctx.isNode()) {
                String task = ctx.getTask();
                if (task != null && task.startsWith("#")) {
                    return PlantumlDisplayResult.of("→ " + task.substring(1));
                }
            } else if (ctx.isLink()) {
                // 隐藏所有 when 条件
                if (ctx.getWhen() != null) {
                    return PlantumlDisplayResult.HIDDEN;
                }
            }
            return PlantumlDisplayResult.ofDefault();
        });
    }

    /**
     * 映射函数：根据节点 ID 做不同处理
     */
    public String case12_byNodeId(Graph graph) {
        return graph.toPlantuml(ctx -> {
            if (ctx.isNode()) {
                String nodeId = ctx.getId();
                String task = ctx.getTask();
                // 特定节点的 task 隐藏
                if ("internalNode".equals(nodeId) && task != null) {
                    return PlantumlDisplayResult.HIDDEN;
                }
            }
            return PlantumlDisplayResult.ofDefault();
        });
    }

    /**
     * 映射函数：根据连接信息做不同处理
     */
    public String case13_byLinkInfo(Graph graph) {
        return graph.toPlantuml(ctx -> {
            if (ctx.isLink()) {
                Link link = ctx.getLink();
                String when = ctx.getWhen();
                // 特定连接的条件特殊处理
                if (link != null && "specialLink".equals(link.getTitle()) && when != null) {
                    return PlantumlDisplayResult.of("【" + when + "】");
                }
            }
            return PlantumlDisplayResult.ofDefault();
        });
    }
}
