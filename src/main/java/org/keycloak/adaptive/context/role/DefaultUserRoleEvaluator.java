package org.keycloak.adaptive.context.role;

import org.keycloak.adaptive.context.ContextUtils;
import org.keycloak.adaptive.evaluator.EvaluatorUtils;
import org.keycloak.adaptive.level.Risk;
import org.keycloak.adaptive.level.Weight;
import org.keycloak.adaptive.spi.context.RiskEvaluator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RoleModel;

import java.util.Optional;

public class DefaultUserRoleEvaluator implements RiskEvaluator {
    private final KeycloakSession session;
    private final UserRoleContext context;
    private Double risk;

    public DefaultUserRoleEvaluator(KeycloakSession session) {
        this.session = session;
        this.context = ContextUtils.getContext(session, UserRoleContextFactory.PROVIDER_ID);
    }

    @Override
    public Optional<Double> getRiskValue() {
        return Optional.ofNullable(risk);
    }

    @Override
    public double getWeight() {
        return EvaluatorUtils.getStoredEvaluatorWeight(session, DefaultUserRoleEvaluatorFactory.class, Weight.NEGLIGIBLE); // Just like that until it's fixed
    }

    @Override
    public boolean isEnabled() {
        return EvaluatorUtils.isEvaluatorEnabled(session, DefaultUserRoleEvaluatorFactory.class);
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public void evaluate() {
        // TODO
        if (context.getData().stream().map(RoleModel::getName).anyMatch(f -> f.equals("ADMIN"))) {
            risk = Risk.INTERMEDIATE;
        } else {
            risk = Risk.SMALL;
        }
    }
}
