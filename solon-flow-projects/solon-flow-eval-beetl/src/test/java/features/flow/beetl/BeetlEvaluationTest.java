package features.flow.beetl;

import org.junit.jupiter.api.Test;
import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.FlowEngine;
import org.noear.solon.flow.driver.SimpleFlowDriver;
import org.noear.solon.flow.evaluation.BeetlEvaluation;

/**
 * @author noear 2025/3/26 created
 */
public class BeetlEvaluationTest {
    @Test
    public void case1() throws Throwable {
        FlowEngine engine = FlowEngine.newInstance();
        engine.register(new SimpleFlowDriver(new BeetlEvaluation()));

        engine.load("classpath:flow/*");

        FlowContext context = new FlowContext();
        context.put("a", 1);
        context.put("b", 2);

        engine.eval("f1", context);
        System.out.println(context.result);
        assert context.result == null;


        context = new FlowContext();
        context.put("a", 3);
        context.put("b", 2);

        engine.eval("f1", context);
        System.out.println(context.result);
        assert ((Number) context.result).intValue() == 5;
    }
}
