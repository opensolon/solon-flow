package features.flow.cfg_script;

import org.junit.jupiter.api.Test;
import org.noear.solon.flow.*;

/**
 * 手动配装风格
 *
 * @author noear 2025/1/10 created
 */
public class ScriptJavaTest {
    private FlowEngine flowEngine = FlowEngine.newInstance();

    @Test
    public void case1() throws Throwable {
        Chain chain = new Chain("c1");

        chain.addNode(NodeDecl.startOf("n1").linkAdd("n2"));
        chain.addNode(NodeDecl.activityOf("n2").task("context.result=111 + a;").linkAdd("n3"));
        chain.addNode(NodeDecl.endOf("n2"));


        FlowContext context = new FlowContext();
        context.put("a", 2);
        context.put("b", 3);
        context.put("c", 4);

        flowEngine.eval(chain, context);
    }
}