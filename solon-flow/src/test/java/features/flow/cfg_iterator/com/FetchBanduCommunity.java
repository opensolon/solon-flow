package features.flow.cfg_iterator.com;

import org.noear.solon.annotation.Component;
import org.noear.solon.flow.FlowExchanger;
import org.noear.solon.flow.Node;
import org.noear.solon.flow.TaskComponent;

import java.util.Arrays;


@Component("fetchBanduCommunity")
public class FetchBanduCommunity implements TaskComponent {
    @Override
    public void run(FlowExchanger context, Node node) throws Throwable {
        context.put("communityIds", Arrays.asList("a1", "a2", "a3"));
    }
}
