package features.flow.aviator;

import org.noear.solon.flow.Evaluation;
import org.noear.solon.flow.driver.AbstractChainDriver;
import org.noear.solon.flow.evaluation.AviatorEvaluation;

/**
 * @author noear 2025/3/26 created
 */
public class AviatorDriver extends AbstractChainDriver {
    @Override
    public Evaluation evaluation() {
        return AviatorEvaluation.INSTANCE;
    }

    @Override
    public Object getComponent(String componentName) {
        return null;
    }
}
