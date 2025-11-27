package demo.flow.grt;

import org.noear.solon.Utils;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Init;
import org.noear.solon.annotation.Inject;
import org.noear.solon.flow.*;

import java.util.List;

/**
 *
 * @author noear 2025/11/26 created
 *
 */
@Component
public class DemoCom {
    //第一个节点 - 处理输入
    @Component("@node_a")
    public static class node_a implements TaskComponent {
        @Override
        public void run(FlowContext context, Node node) throws Throwable {
            String user_input = context.getAs("input");
            List<String> current_messages = context.getAs("messages");

            context.put("output", "节点A处理: " + user_input.toUpperCase());
            current_messages.add("经过节点A: " + user_input);
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
            current_messages.add("经过节点B");
        }
    }


    @Init
    public void main() {
        FlowContext initial_state = FlowContext.of()
                .put("input", "a")
                .put("output", "b")
                .put("messages", Utils.asList());

        flowEngine.eval("demo1", initial_state);
    }

    @Inject
    FlowEngine flowEngine;
}
