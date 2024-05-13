/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.adaptive.context.os;

import org.keycloak.adaptive.context.ContextUtils;
import org.keycloak.adaptive.evaluator.EvaluatorUtils;
import org.keycloak.adaptive.level.Risk;
import org.keycloak.adaptive.spi.context.RiskEvaluator;
import org.keycloak.models.KeycloakSession;

import java.util.Optional;

public class OperatingSystemRiskEvaluator implements RiskEvaluator {
    private final KeycloakSession session;
    private final OperatingSystemCondition condition;
    private Double risk;

    public OperatingSystemRiskEvaluator(KeycloakSession session) {
        this.session = session;
        this.condition = ContextUtils.getContextCondition(session, OperatingSystemConditionFactory.PROVIDER_ID);
    }

    @Override
    public Optional<Double> getRiskValue() {
        return Optional.ofNullable(risk);
    }

    @Override
    public double getWeight() {
        return EvaluatorUtils.getStoredEvaluatorWeight(session, OperatingSystemRiskEvaluatorFactory.class);
    }

    @Override
    public boolean isEnabled() {
        return EvaluatorUtils.isEvaluatorEnabled(session, OperatingSystemRiskEvaluatorFactory.class);
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public void evaluate() {
        if (condition.isOs(DefaultOperatingSystems.LINUX)) {
            this.risk = Risk.INTERMEDIATE; // they say that the probability of attacks is higher from Linux devices xD
        } else if (condition.isDefaultKnownOs()) {
            this.risk = Risk.SMALL;
        } else {
            this.risk = Risk.INTERMEDIATE;
        }
    }
}
