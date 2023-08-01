package com.farao_community.farao.swe.runner.app.dichotomy.shift;

import com.powsybl.iidm.network.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
public class ConnectTopologyVisitor implements TopologyVisitor {
    private final List<Connectable> connectables;

    public ConnectTopologyVisitor() {
        this.connectables = new ArrayList<>();
    }

    @Override
    public void visitBusbarSection(BusbarSection busbarSection) {
        connectables.add(busbarSection);
    }

    @Override
    public void visitLine(Line line, Branch.Side side) {
        connectables.add(line);
    }

    @Override
    public void visitTwoWindingsTransformer(TwoWindingsTransformer twoWindingsTransformer, Branch.Side side) {
        connectables.add(twoWindingsTransformer);
    }

    @Override
    public void visitThreeWindingsTransformer(ThreeWindingsTransformer threeWindingsTransformer, ThreeWindingsTransformer.Side side) {
        connectables.add(threeWindingsTransformer);
    }

    @Override
    public void visitGenerator(Generator generator) {
        connectables.add(generator);

    }

    @Override
    public void visitLoad(Load load) {
        connectables.add(load);
    }

    @Override
    public void visitShuntCompensator(ShuntCompensator shuntCompensator) {
        connectables.add(shuntCompensator);
    }

    @Override
    public void visitDanglingLine(DanglingLine danglingLine) {
        connectables.add(danglingLine);

    }

    @Override
    public void visitStaticVarCompensator(StaticVarCompensator staticVarCompensator) {
        connectables.add(staticVarCompensator);
    }

    public List<Connectable> getConnectables() {
        return connectables;
    }
}
