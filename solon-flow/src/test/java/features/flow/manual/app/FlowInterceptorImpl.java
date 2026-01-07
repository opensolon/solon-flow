package features.flow.manual.app;

import org.noear.solon.annotation.Component;
import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.Node;
import org.noear.solon.flow.intercept.FlowInterceptor;
import org.noear.solon.flow.intercept.FlowInvocation;

/**
 * @author noear 2025/3/4 created
 */
@Component
public class FlowInterceptorImpl implements FlowInterceptor {
    @Override
    public void interceptFlow(FlowInvocation invocation)  {
        System.out.println("doIntercept---------------");
        invocation.invoke();
    }

    @Override
    public void onNodeStart(FlowContext context, Node node) {

    }

    @Override
    public void onNodeEnd(FlowContext context, Node node) {

    }
}
