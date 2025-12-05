package features.flow.stateful;

import org.junit.jupiter.api.Test;
import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.FlowEngine;
import org.noear.solon.flow.Graph;
import org.noear.solon.flow.stateful.StateController;
import org.noear.solon.flow.stateful.StateRepository;
import org.noear.solon.flow.stateful.StateResult;
import org.noear.solon.flow.stateful.controller.NotBlockStateController;
import org.noear.solon.flow.stateful.repository.InMemoryStateRepository;

/**
 *
 * @author noear 2025/12/5 created
 *
 */
public class HelloTest {
    @Test
    public void case1() {
        FlowEngine engine = FlowEngine.newInstance();
        StateController stateController = new NotBlockStateController();
        StateRepository stateRepository = new InMemoryStateRepository();

        Graph graph = Graph.create("c1", decl -> {
            decl.addStart("n1").linkAdd("n2");
            decl.addActivity("n2").task("System.out.println(\"hello world!\");").linkAdd("n3");
            decl.addEnd("n3");
        });

        StateResult result = engine.forStateful()
                .eval(graph, FlowContext.of("i-1", stateController, stateRepository));

        System.out.println(result.getNode().getId());
        System.out.println(result.getState());
    }
}
