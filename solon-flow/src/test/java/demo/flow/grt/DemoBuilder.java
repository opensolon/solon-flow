package demo.flow.grt;

import org.noear.solon.Utils;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Init;
import org.noear.solon.annotation.Inject;
import org.noear.solon.flow.*;

import java.util.Arrays;
import java.util.List;

/**
 *
 * @author noear 2025/11/26 created
 *
 */
@Component
public class DemoBuilder {
    //第一个节点 - 处理输入
    @Component("@node_a")
    public static class node_a implements TaskComponent {
        @Override
        public void run(FlowContext context, Node node) throws Throwable {
            String user_input = context.getAs("input");

            context.put("output", "节点A处理: " + user_input.toUpperCase());
            context.put("messages", Arrays.asList("经过节点A: " + user_input));
        }
    }

    //第二个节点 - 进一步处理
    @Component("@node_b")
    public static class node_b implements TaskComponent {
        @Override
        public void run(FlowContext context, Node node) throws Throwable {
            String current_output = context.getAs("output");
            List<String> current_messages = context.getAs("messages");

            context.put("output", current_output + "-> 节点B处理");
            context.put("messages", Arrays.asList(current_messages, "经过节点B"));
        }
    }

    public Graph create_simple_workflow() {
        Graph workflow = new GraphDecl("demo1").create(decl->{
            decl.addNode(NodeDecl.activityOf("node_a").task("@node_a").linkAdd("node_b"));
            decl.addNode(NodeDecl.activityOf("node_b").task("@node_b").linkAdd("end"));
            decl.addNode(NodeDecl.endOf("end"));
        });

        return workflow;
    }

    @Init
    public void main() {
        Graph graph = create_simple_workflow();

        FlowContext initial_state = FlowContext.of()
                .put("input", "a")
                .put("output", "b")
                .put("messages", Utils.asList());

        flowEngine.eval(graph, initial_state);
    }

    @Inject
    FlowEngine flowEngine;
}
