package com.farao_community.farao.swe.runner.app.domain;

import com.farao_community.farao.gridcapa.task_manager.api.TaskParameterDto;
import com.farao_community.farao.gridcapa_swe_commons.exception.SweInvalidDataException;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class SweTaskParameters {

    private static final String IS_RUN_ES_FR = "RUN_ES-FR";
    private static final String IS_RUN_ES_PT = "RUN_ES-PT";
    private static final String IS_RUN_FR_ES = "RUN_FR-ES";
    private static final String IS_RUN_PT_ES = "RUN_PT-ES";

    private boolean runDirectionEsToFr;
    private boolean runDirectionEsToPt;
    private boolean runDirectionFrToEs;
    private boolean runDirectionPtToEs;

    public SweTaskParameters(List<TaskParameterDto> parameters) {
        for (TaskParameterDto parameter : parameters) {
            switch (parameter.getId()) {
                case IS_RUN_ES_FR -> runDirectionEsToFr = validateIsBooleanAndGet(parameter);
                case IS_RUN_ES_PT -> runDirectionEsToPt = validateIsBooleanAndGet(parameter);
                case IS_RUN_FR_ES -> runDirectionFrToEs = validateIsBooleanAndGet(parameter);
                case IS_RUN_PT_ES -> runDirectionPtToEs = validateIsBooleanAndGet(parameter);
                default -> {
                    //do nothing
                }
            }
        }
    }

    private boolean validateIsBooleanAndGet(TaskParameterDto parameter) {
        if (StringUtils.equals("BOOLEAN", parameter.getParameterType())) {
            return Boolean.parseBoolean(parameter.getValue());
        } else {
            throw new SweInvalidDataException(String.format("Invalid boolean parameter with id %s and value %s", parameter.getId(), parameter.getValue()));
        }
    }

    public boolean isRunDirectionEsToFr() {
        return runDirectionEsToFr;
    }

    public boolean isRunDirectionEsToPt() {
        return runDirectionEsToPt;
    }

    public boolean isRunDirectionFrToEs() {
        return runDirectionFrToEs;
    }

    public boolean isRunDirectionPtToEs() {
        return runDirectionPtToEs;
    }

}
