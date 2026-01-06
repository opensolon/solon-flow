package features.flow.manual.cfg_loop.com;

import org.noear.solon.annotation.Component;
import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.Node;
import org.noear.solon.flow.TaskComponent;

import java.util.Arrays;


@Component("fetchBanduAlbum")
public class FetchBanduAlbum implements TaskComponent {
    @Override
    public void run(FlowContext context, Node node) throws Throwable {
        String communityId = context.getAs("communityId");
        context.put("albumIds", Arrays.asList(communityId + "_b1", communityId + "_b2", communityId + "_b3"));
    }
}
