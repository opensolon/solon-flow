package demo.flow.stateful;

import org.noear.solon.annotation.Bean;
import org.noear.solon.annotation.Configuration;
import org.noear.solon.flow.stateful.StatefulFlowEngineDefault;
import org.noear.solon.flow.stateful.driver.StatefulSimpleFlowDriver;
import org.noear.solon.flow.stateful.controller.MetaStateController;
import org.noear.solon.flow.stateful.repository.InMemoryStateRepository;

/**
 * @author noear 2025/3/27 created
 */
@Configuration
public class DemoConfig {
    @Bean
    public StatefulFlowEngineDefault flowEngine() {
        //初始化引擎
        return new StatefulFlowEngineDefault(StatefulSimpleFlowDriver.builder()
                .stateController(new MetaStateController())
                .stateRepository(new InMemoryStateRepository())
                .build());
    }
}
