package features.flow.manual.app;

import org.noear.solon.annotation.Bean;
import org.noear.solon.annotation.Configuration;
import org.noear.solon.flow.FlowEngine;
import org.noear.solon.flow.driver.SimpleFlowDriver;

/**
 * @author noear 2025/4/7 created
 */
@Configuration
public class AppConfig {
    @Bean
    public FlowEngine flowEngine() {
        //可以替换掉默认的引擎
        return FlowEngine.newInstance(SimpleFlowDriver.builder().build());
    }
}
