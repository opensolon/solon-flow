package features.flow.cfg_loop.com;

import org.noear.solon.annotation.Component;
import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.Node;
import org.noear.solon.flow.TaskComponent;


@Component("downloadBanduAttachment")
public class DownloadBanduAttachment implements TaskComponent {
    @Override
    public void run(FlowContext context, Node node) throws Throwable {
        String courseId = context.getAs("courseId");
        context.put(this.getClass().getSimpleName() + "_" + courseId, courseId);
    }
}
