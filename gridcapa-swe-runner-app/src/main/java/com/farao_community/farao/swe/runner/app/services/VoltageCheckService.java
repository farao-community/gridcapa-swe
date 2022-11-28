package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.monitoring.voltage_monitoring.VoltageMonitoring;
import com.farao_community.farao.monitoring.voltage_monitoring.VoltageMonitoringResult;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.farao_community.farao.swe.runner.app.dichotomy.DichotomyDirection;
import com.farao_community.farao.swe.runner.app.dichotomy.NetworkShifterProvider;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * @author Marc Schwitzguébel {@literal <marc.schwitzguebel at rte-france.com>}
 */

@Service
public class VoltageCheckService {
    private final Logger businessLogger;

    public VoltageCheckService(Logger businessLogger) {
        this.businessLogger = businessLogger;
    }

    public Optional<VoltageMonitoringResult> runVoltageCheck(SweData sweData, DichotomyResult<RaoResponse> dichotomyResult, DichotomyDirection direction) {

        if ((direction == DichotomyDirection.ES_FR || direction == DichotomyDirection.FR_ES) && dichotomyResult.hasValidStep()) {
            businessLogger.info("[{}] : Running voltage check", direction);
            Crac crac = sweData.getCracFrEs().getCrac();
            Network network = NetworkShifterProvider.getNetworkByDirection(sweData, direction);
            VoltageMonitoring voltageMonitoring = new VoltageMonitoring(crac, network, dichotomyResult.getHighestValidStep().getRaoResult());
            return Optional.of(voltageMonitoring.run(LoadFlow.find().getName(), LoadFlowParameters.load(), 4));
        }
        return Optional.empty();
    }
}
