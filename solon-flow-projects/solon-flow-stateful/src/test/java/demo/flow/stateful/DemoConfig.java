package demo.flow.stateful;

import org.noear.solon.annotation.Bean;
import org.noear.solon.annotation.Configuration;
import org.noear.solon.flow.stateful.repository.InMemoryStateRepository;
import org.noear.solon.flow.stateful.StatefulFlowEngine;
import org.noear.solon.flow.stateful.StatefulSimpleFlowDriver;

/**
 * @author noear 2025/3/27 created
 */
@Configuration
public class DemoConfig {
    @Bean
    public StatefulFlowEngine flowEngine() {
        //初始化引擎
        return new StatefulFlowEngine(
                new StatefulSimpleFlowDriver(
                        new InMemoryStateRepository()));
    }
}
