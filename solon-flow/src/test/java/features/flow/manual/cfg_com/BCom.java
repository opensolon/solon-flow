package features.flow.manual.cfg_com;

import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Component;
import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.Node;
import org.noear.solon.flow.TaskComponent;

/**
 * @author noear 2025/1/11 created
 */
@Slf4j
@Component("b")
public class BCom implements TaskComponent {
    @Override
    public void run(FlowContext context, Node node) {
        log.info("BCom");
    }
}
