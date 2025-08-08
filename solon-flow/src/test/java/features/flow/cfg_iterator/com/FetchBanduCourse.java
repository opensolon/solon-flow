package features.flow.cfg_iterator.com;

import org.noear.solon.annotation.Component;
import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.Node;
import org.noear.solon.flow.TaskComponent;

import java.util.Arrays;


@Component("fetchBanduCourse")
public class FetchBanduCourse implements TaskComponent {
    @Override
    public void run(FlowContext context, Node node) throws Throwable {
        String albumId = context.get("albumId");
        context.put("courseIds", Arrays.asList(albumId + "-c1", albumId + "-c2", albumId + "-c3"));
    }
}
