package features.flow.stateful;

import org.junit.jupiter.api.Test;
import org.noear.solon.Utils;
import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.FlowEngine;
import org.noear.solon.flow.Node;
import org.noear.solon.flow.container.MapContainer;
import org.noear.solon.flow.stateful.*;
import org.noear.solon.flow.stateful.controller.ActorStateController;
import org.noear.solon.flow.driver.SimpleFlowDriver;
import org.noear.solon.flow.stateful.repository.InMemoryStateRepository;
import org.noear.solon.test.SolonTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author noear 2025/8/4 created
 */
@SolonTest
public class JumpFlowTest {
    static final Logger log = LoggerFactory.getLogger(JumpFlowTest.class);

    final String graphId = "test2";
    final String instanceId = Utils.uuid();
    final String actor = "role";

    StateRepository stateRepository = new InMemoryStateRepository();
    ActorStateController stateController = new ActorStateController(actor) {
        @Override
        public boolean isOperatable(FlowContext context, Node node) {
            if ("admin".equals(context.getAs(actor))) {
                return true;
            }

            return super.isOperatable(context, node);
        }
    };

    private FlowStatefulService buildStatefulService() {
        MapContainer container = new MapContainer();
        FlowEngine fe = FlowEngine.newInstance(SimpleFlowDriver.builder()
                .container(container)
                .build());


        fe.load("classpath:flow/stateful/*.yml");

        return fe.forStateful();
    }

    @Test
    public void case1() {
        FlowStatefulService statefulService = buildStatefulService();
        FlowContext context = FlowContext.of(instanceId, stateController, stateRepository).put(actor, "admin");

        statefulService.postOperation(graphId, "n3", Operation.FORWARD_JUMP, context);

        StatefulTask task = statefulService.getTask(graphId, context);

        log.debug(task.toString());
        assert task.getState() == StateType.WAITING;
        assert task.getNode().getId().equals("n4");


        statefulService.postOperation(graphId, "n1", Operation.BACK_JUMP, context);

        task = statefulService.getTask(graphId, context);

        log.debug(task.toString());
        assert task.getState() == StateType.WAITING;
        assert task.getNode().getId().equals("n0");
    }
}