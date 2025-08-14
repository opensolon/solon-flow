package features.flow.cfg_iterator.com;

import org.noear.solon.annotation.Component;
import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.FlowExchanger;
import org.noear.solon.flow.Node;
import org.noear.solon.flow.TaskComponent;

import java.util.Arrays;


@Component("fetchBanduAlbum")
public class FetchBanduAlbum implements TaskComponent {
    @Override
    public void run(FlowContext context, Node node) throws Throwable {
        String communityId = context.get("communityId");
        context.put("albumIds", Arrays.asList(communityId + "-b1", communityId + "-b2", communityId + "-b3"));
    }
}
