package features.flow.stateful;

import org.junit.jupiter.api.Test;
import org.noear.solon.Utils;
import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.FlowEngine;
import org.noear.solon.flow.Node;
import org.noear.solon.flow.container.MapContainer;
import org.noear.solon.flow.stateful.FlowStatefulService;
import org.noear.solon.flow.stateful.Operation;
import org.noear.solon.flow.stateful.StateType;
import org.noear.solon.flow.stateful.StatefulTask;
import org.noear.solon.flow.stateful.controller.ActorStateController;
import org.noear.solon.flow.stateful.StatefulFlowDriver;
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

    final String chainId = "test2";
    final String instanceId = Utils.uuid();
    final String actor = "role";

    ActorStateController stateController = new ActorStateController(actor) {
        @Override
        public boolean isOperatable(FlowContext context, Node node) {
            if ("admin".equals(context.get(actor))) {
                return true;
            }

            return super.isOperatable(context, node);
        }
    };
    InMemoryStateRepository stateRepository = new InMemoryStateRepository();

    private FlowStatefulService buildStatefulService() {
        MapContainer container = new MapContainer();
        FlowEngine fe = FlowEngine.newInstance(StatefulFlowDriver.builder()
                .container(container)
                .build());


        fe.load("classpath:flow/stateful/*.yml");

        return fe.statefulService();
    }

    @Test
    public void case1() {
        FlowStatefulService statefulService = buildStatefulService();
        FlowContext context = FlowContext.of(instanceId, stateController, stateRepository).put(actor, "admin");

        statefulService.postOperation(context, chainId, "n3", Operation.FORWARD_JUMP);

        StatefulTask task = statefulService.getTask(chainId, context);

        log.debug(task.toString());
        assert task.getState() == StateType.WAITING;
        assert task.getNode().getId().equals("n4");


        statefulService.postOperation(context, chainId, "n1", Operation.BACK_JUMP);

        task = statefulService.getTask(chainId, context);

        log.debug(task.toString());
        assert task.getState() == StateType.WAITING;
        assert task.getNode().getId().equals("n0");
    }
}