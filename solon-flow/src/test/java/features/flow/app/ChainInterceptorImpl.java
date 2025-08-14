package features.flow.app;

import org.noear.solon.annotation.Component;
import org.noear.solon.flow.FlowExchanger;
import org.noear.solon.flow.Node;
import org.noear.solon.flow.intercept.ChainInterceptor;
import org.noear.solon.flow.intercept.ChainInvocation;

/**
 * @author noear 2025/3/4 created
 */
@Component
public class ChainInterceptorImpl implements ChainInterceptor {
    @Override
    public void doIntercept(ChainInvocation invocation)  {
        System.out.println("doIntercept---------------");
        invocation.invoke();
    }

    @Override
    public void onNodeStart(FlowExchanger exchanger, Node node) {

    }

    @Override
    public void onNodeEnd(FlowExchanger exchanger, Node node) {

    }
}
