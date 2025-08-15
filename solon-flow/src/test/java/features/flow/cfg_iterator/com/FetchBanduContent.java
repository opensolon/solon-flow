package features.flow.cfg_iterator.com;

import org.noear.solon.annotation.Component;
import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.Node;
import org.noear.solon.flow.TaskComponent;


@Component("fetchBanduContent")
public class FetchBanduContent implements TaskComponent {
    @Override
    public void run(FlowContext context, Node node) throws Throwable {
        String courseId = context.getAs("courseId");

        context.put(this.getClass().getSimpleName() + "_" + courseId, courseId);
    }
}
