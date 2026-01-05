package demo.workflow;

import org.noear.solon.annotation.Bean;
import org.noear.solon.annotation.Configuration;
import org.noear.solon.flow.workflow.StateController;
import org.noear.solon.flow.workflow.StateRepository;
import org.noear.solon.flow.workflow.controller.ActorStateController;
import org.noear.solon.flow.workflow.repository.InMemoryStateRepository;

/**
 * @author noear 2025/3/27 created
 */
@Configuration
public class DemoConfig {
    @Bean
    public StateController stateController() {
        return new ActorStateController();
    }

    @Bean
    public StateRepository stateRepository() {
        return new InMemoryStateRepository();
    }
}
