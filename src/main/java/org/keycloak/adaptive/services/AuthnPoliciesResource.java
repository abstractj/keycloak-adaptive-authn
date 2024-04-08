package org.keycloak.adaptive.services;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.keycloak.adaptive.models.AuthnPolicyModel;
import org.keycloak.adaptive.models.AuthnPolicyRepresentation;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.utils.ReservedCharValidator;
import org.keycloak.utils.StringUtil;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Provider
public class AuthnPoliciesResource implements RealmResourceProvider {
    private final KeycloakSession session;
    private final RealmModel realm;

    public AuthnPoliciesResource(KeycloakSession session) {
        this.session = session;
        this.realm = session.getContext().getRealm();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Set<AuthnPolicyRepresentation> getPolicies() {
        return realm.getAuthenticationFlowsStream()
                .filter(AuthenticationFlowModel::isTopLevel)
                .map(f -> ModelToRepresentation.toRepresentation(session, realm, f))
                .map(f -> (AuthnPolicyRepresentation) f)
                .collect(Collectors.toSet());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addPolicy(AuthnPolicyRepresentation policy) {
        if (StringUtil.isBlank(policy.getAlias())) {
            throw ErrorResponse.exists("Failed to create policy with empty alias name");
        }

        if (realm.getFlowByAlias(policy.getAlias()) != null) {
            throw ErrorResponse.exists("Policy " + policy.getAlias() + " already exists");
        }

        //adding an empty string to avoid NPE
        if (Objects.isNull(policy.getDescription())) {
            policy.setDescription("");
        }

        policy.setDescription("POLICY - " + policy.getDescription());

        if (!policy.isTopLevel()) {
            throw new BadRequestException("Authentication policy must be top level flow");
        }

        ReservedCharValidator.validate(policy.getAlias());

        AuthenticationFlowModel createdModel = realm.addAuthenticationFlow(RepresentationToModel.toModel(policy));
        policy.setId(createdModel.getId());
        return Response.created(session.getContext().getUri().getAbsolutePathBuilder().path(policy.getId()).build()).build();
    }

    @Path("/{policyId}")
    public AuthnPolicyResource forwardToPolicyResource(@PathParam("policyId") String policyId) {
        var policy = (AuthnPolicyModel) realm.getAuthenticationFlowById(policyId);
        if (policy == null) {
            throw new NotFoundException("Could not find policy by id");
        }
        return new AuthnPolicyResource(session, policy);
    }

    @Override
    public Object getResource() {
        return this;
    }

    @Override
    public void close() {

    }

    public static int getNextPriority(RealmModel realm, AuthnPolicyModel parentPolicy) {
        var conditions = realm.getAuthenticationExecutionsStream(parentPolicy.getId()).toList();
        return conditions.isEmpty() ? 0 : conditions.get(conditions.size() - 1).getPriority() + 1;
    }
}