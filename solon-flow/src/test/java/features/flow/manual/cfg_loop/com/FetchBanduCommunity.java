package features.flow.manual.cfg_loop.com;

import org.noear.solon.annotation.Component;
import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.Node;
import org.noear.solon.flow.TaskComponent;

import java.util.Arrays;


@Component("fetchBanduCommunity")
public class FetchBanduCommunity implements TaskComponent {
    @Override
    public void run(FlowContext context, Node node) throws Throwable {
        context.put("communityIds", Arrays.asList("a1", "a2", "a3"));
    }
}
