package features.flow.aviator;

import org.junit.jupiter.api.Test;
import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.FlowEngine;
import org.noear.solon.flow.driver.SimpleFlowDriver;
import org.noear.solon.flow.evaluation.AviatorEvaluation;

/**
 * @author noear 2025/3/26 created
 */
public class AviatorEvaluationTest {
    @Test
    public void case1() throws Throwable {
        FlowEngine engine = FlowEngine.newInstance();
        engine.register(new SimpleFlowDriver(new AviatorEvaluation()));

        engine.load("classpath:flow/*");

        FlowContext context = FlowContext.of();
        context.put("a", 1);
        context.put("b", 2);

        engine.eval("f1", context);
        System.out.println(context.getAsObject("result"));
        assert context.get("result") == null;


        context = FlowContext.of();
        context.put("a", 3);
        context.put("b", 2);

        engine.eval("f1", context);
        System.out.println(context.getAsObject("result"));
        assert context.getAsNumber("result").intValue() == 5;
    }

    //demo
    public void case2() throws Throwable {
        FlowEngine engine = FlowEngine.newInstance();
        engine.register(new SimpleFlowDriver(new SpringContainer()));
    }
}
