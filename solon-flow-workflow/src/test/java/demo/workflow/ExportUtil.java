package demo.workflow;

import org.noear.snack4.ONode;
import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.Graph;
import org.noear.solon.flow.Node;
import org.noear.solon.flow.workflow.TaskState;
import org.noear.solon.flow.workflow.WorkflowService;
import org.yaml.snakeyaml.Yaml;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author noear 2025/12/21 created
 *
 */
public class ExportUtil {
    /**
     * 转为 yaml
     */
    public static String toYaml(Map<String, Object> map) {
        return new Yaml().dump(map);
    }

    /**
     * 转为 json
     */
    public static String toJson(Map<String, Object> map) {
        return ONode.serialize(map);
    }


    public static Map<String, Object> buildGraphDom1(WorkflowService workflow, Graph graph, FlowContext context) {
        Map<String, Object> domRoot = graph.toMap();

        if (context != null) {
            Map<String, String> domState = new LinkedHashMap<>();
            domRoot.put("state", domState); //在 layout 之外，新增一个根级的 state 节点（用它放节点状态）

            for (Map.Entry<String, Node> entry : graph.getNodes().entrySet()) {
                TaskState type = workflow.getState(entry.getValue(), context);
                if (type != null && type != TaskState.UNKNOWN) {
                    domState.put(entry.getKey(), type.toString());
                }
            }
        }

        return domRoot;
    }

    public static Map<String, Object> buildGraphDom2(WorkflowService workflow, Graph graph, FlowContext context) {
        return Graph.copy(graph, spec -> {
            for (Map.Entry<String, Node> entry : graph.getNodes().entrySet()) {
                TaskState type = workflow.getState(entry.getValue(), context);
                if (type != null && type != TaskState.UNKNOWN) {
                    spec.getNode(entry.getKey()).metaPut("$state", type.toString()); //直接在元数据上添加状态
                }
            }
        }).toMap();
    }
}