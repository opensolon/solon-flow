package demo.flow.stateful;

import org.noear.solon.annotation.Bean;
import org.noear.solon.annotation.Configuration;
import org.noear.solon.flow.stateful.operator.MetaStateOperator;
import org.noear.solon.flow.stateful.StatefulFlowEngine;
import org.noear.solon.flow.stateful.StatefulSimpleFlowDriver;
import org.noear.solon.flow.stateful.repository.InMemoryStateRepository;

/**
 * @author noear 2025/3/27 created
 */
@Configuration
public class DemoConfig {
    @Bean
    public StatefulFlowEngine flowEngine() {
        //初始化引擎
        return new StatefulFlowEngine(StatefulSimpleFlowDriver.builder()
                .stateOperator(new MetaStateOperator())
                .stateRepository(new InMemoryStateRepository())
                .build());
    }
}
