package demo.flow;

import org.noear.solon.Utils;
import org.noear.solon.flow.Graph;
import org.noear.solon.flow.Link;
import org.noear.solon.flow.Node;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author noear 2026/3/22 created
 *
 */
public class PumlDemo {
    //使用自带方法
    public String case1(Graph graph) {
        return graph.toPlantuml();
    }

    //自定义生成
    public String case2(Graph graph) {
        StringBuilder sb = new StringBuilder();
        sb.append("@startuml\n");

        // 全局样式优化，让状态图具有活动图的视觉美感
        sb.append("skinparam shadowing false\n");
        sb.append("skinparam state {\n")
                .append("  BackgroundColor White\n")
                .append("  BorderColor #333333\n")
                .append("  FontName SansSerif\n")
                .append("  BackgroundColor<<Gateway>> #fff9c4\n") // 网关用淡黄色
                .append("  BorderColor<<Gateway>> #fbc02d\n")
                .append("}\n");

        if (Utils.isNotEmpty(graph.getTitle())) {
            sb.append("title ").append(graph.getTitle()).append("\n");
        }

        // 1. 声明节点：遍历 nodes
        for (Node node : graph.getNodes().values()) {
            String nodeId = node.getId();
            String title = Utils.isNotEmpty(node.getTitle()) ? node.getTitle() : nodeId;

            // 渲染逻辑
            switch (node.getType()) {
                case START:
                    // 开始节点
                    sb.append("state ").append(nodeId).append(" <<start>>\n");
                    sb.append(nodeId).append(" : ").append(title).append("\n");
                    break;
                case END:
                    // 结束节点
                    sb.append("state ").append(nodeId).append(" <<end>>\n");
                    sb.append(nodeId).append(" : ").append(title).append("\n");
                    break;
                case EXCLUSIVE:
                case INCLUSIVE:
                case PARALLEL:
                case LOOP:
                    // 网关节点：使用 choice 刻板印象显示为菱形
                    sb.append("state ").append(nodeId).append(" <<choice>> <<Gateway>>\n");
                    sb.append(nodeId).append(" : ").append(node.getType().name()).append("\n");
                    break;
                default:
                    // 业务活动节点：处理引号和描述信息
                    // 使用别名定义，防止 title 中的特殊字符导致语法错误
                    sb.append("state \"").append(title).append("\" as ").append(nodeId).append("\n");
                    if (Utils.isNotEmpty(node.getTask().getDescription())) {
                        // 将任务描述作为状态内部的说明
                        sb.append(nodeId).append(" : ").append(node.getTask().getDescription()).append("\n");
                    }
                    break;
            }
        }

        // 2. 声明连接：遍历 links
        for (Link link : graph.getLinks()) {
            // 基本语法: prevId --> nextId
            sb.append(link.getPrevId()).append(" --> ").append(link.getNextId());

            // 拼接连线上的标题或条件描述 (When)
            List<String> labels = new ArrayList<>();
            if (Utils.isNotEmpty(link.getTitle())) {
                labels.add(link.getTitle());
            }
            if (Utils.isNotEmpty(link.getWhen().getDescription())) {
                labels.add("[" + link.getWhen().getDescription() + "]");
            }

            if (!labels.isEmpty()) {
                sb.append(" : ").append(String.join(" ", labels));
            }
            sb.append("\n");
        }

        sb.append("@enduml");
        return sb.toString();
    }
}