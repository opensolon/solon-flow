package demo.flow.stateful;

import org.noear.solon.annotation.Bean;
import org.noear.solon.annotation.Configuration;
import org.noear.solon.flow.FlowEngine;
import org.noear.solon.flow.stateful.StateController;
import org.noear.solon.flow.stateful.StateRepository;
import org.noear.solon.flow.stateful.driver.StatefulSimpleFlowDriver;
import org.noear.solon.flow.stateful.controller.ActorStateController;
import org.noear.solon.flow.stateful.repository.InMemoryStateRepository;

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
