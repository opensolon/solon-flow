package features.flow.manual.cfg_loop.com;

import org.noear.solon.annotation.Component;
import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.Node;
import org.noear.solon.flow.TaskComponent;

import java.util.Arrays;


@Component("fetchBanduCourse")
public class FetchBanduCourse implements TaskComponent {
    @Override
    public void run(FlowContext context, Node node) throws Throwable {
        String albumId = context.getAs("albumId");
        context.put("courseIds", Arrays.asList(albumId + "_c1", albumId + "_c2", albumId + "_c3"));
    }
}
