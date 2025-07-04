package demo.flow.stateful;

import org.noear.solon.annotation.Bean;
import org.noear.solon.annotation.Configuration;
import org.noear.solon.flow.FlowEngine;
import org.noear.solon.flow.stateful.driver.StatefulSimpleFlowDriver;
import org.noear.solon.flow.stateful.controller.ActorStateController;
import org.noear.solon.flow.stateful.repository.InMemoryStateRepository;

/**
 * @author noear 2025/3/27 created
 */
@Configuration
public class DemoConfig {
    @Bean
    public FlowEngine flowEngine() {
        //初始化引擎
        return FlowEngine.newInstance(StatefulSimpleFlowDriver.builder()
                .stateController(new ActorStateController())
                .stateRepository(new InMemoryStateRepository())
                .build());
    }
}
