package features.flow.beetl;

import org.noear.solon.flow.Evaluation;
import org.noear.solon.flow.driver.AbstractChainDriver;
import org.noear.solon.flow.evaluation.BeetlEvaluation;

/**
 * @author noear 2025/3/26 created
 */
public class BeetlDriver extends AbstractChainDriver {
    @Override
    public Evaluation evaluation() {
        return BeetlEvaluation.INSTANCE;
    }

    @Override
    public Object getComponent(String componentName) {
        return null;
    }
}
