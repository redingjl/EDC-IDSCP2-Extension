/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.negotiation.api.controller;

import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreementMessage;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreementVerificationMessage;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractNegotiationEventMessage;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationTerminationMessage;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequestMessage;
import org.eclipse.edc.connector.contract.spi.types.protocol.ContractRemoteMessage;
import org.eclipse.edc.connector.spi.contractnegotiation.ContractNegotiationProtocolService;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.protocol.dsp.DspError;
import org.eclipse.edc.service.spi.result.ServiceFailure;
import org.eclipse.edc.service.spi.result.ServiceResult;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static java.lang.String.format;
import static org.eclipse.edc.jsonld.spi.TypeUtil.isOfExpectedType;
import static org.eclipse.edc.protocol.dsp.DspErrorDetails.NOT_IMPLEMENTED;
import static org.eclipse.edc.protocol.dsp.negotiation.api.NegotiationApiPaths.AGREEMENT;
import static org.eclipse.edc.protocol.dsp.negotiation.api.NegotiationApiPaths.BASE_PATH;
import static org.eclipse.edc.protocol.dsp.negotiation.api.NegotiationApiPaths.CONTRACT_OFFER;
import static org.eclipse.edc.protocol.dsp.negotiation.api.NegotiationApiPaths.CONTRACT_REQUEST;
import static org.eclipse.edc.protocol.dsp.negotiation.api.NegotiationApiPaths.EVENT;
import static org.eclipse.edc.protocol.dsp.negotiation.api.NegotiationApiPaths.INITIAL_CONTRACT_REQUEST;
import static org.eclipse.edc.protocol.dsp.negotiation.api.NegotiationApiPaths.TERMINATION;
import static org.eclipse.edc.protocol.dsp.negotiation.api.NegotiationApiPaths.VERIFICATION;
import static org.eclipse.edc.protocol.dsp.spi.types.HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP;
import static org.eclipse.edc.protocol.dsp.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_AGREEMENT_MESSAGE;
import static org.eclipse.edc.protocol.dsp.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_AGREEMENT_VERIFICATION_MESSAGE;
import static org.eclipse.edc.protocol.dsp.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_NEGOTIATION_ERROR;
import static org.eclipse.edc.protocol.dsp.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_NEGOTIATION_EVENT_MESSAGE;
import static org.eclipse.edc.protocol.dsp.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_NEGOTIATION_TERMINATION_MESSAGE;
import static org.eclipse.edc.protocol.dsp.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_REQUEST_MESSAGE;
import static org.eclipse.edc.protocol.dsp.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CALLBACK_ADDRESS;

/**
 * Provides consumer and provider endpoints for the contract negotiation according to the http binding
 * of the dataspace protocol.
 */
@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@Path(BASE_PATH)
public class DspNegotiationApiController {

    private final IdentityService identityService;
    private final TypeTransformerRegistry transformerRegistry;
    private final String callbackAddress;
    private final Monitor monitor;
    private final ContractNegotiationProtocolService protocolService;
    private final JsonLd jsonLdService;

    public DspNegotiationApiController(String callbackAddress,
                                       IdentityService identityService,
                                       TypeTransformerRegistry transformerRegistry,
                                       ContractNegotiationProtocolService protocolService,
                                       JsonLd jsonLdService,
                                       Monitor monitor) {
        this.callbackAddress = callbackAddress;
        this.identityService = identityService;
        this.monitor = monitor;
        this.protocolService = protocolService;
        this.transformerRegistry = transformerRegistry;
        this.jsonLdService = jsonLdService;
    }

    /**
     * Provider-specific endpoint.
     *
     * @param id    of contract negotiation.
     * @param token identity token.
     * @return the requested contract negotiation or an error.
     */
    @GET
    @Path("{id}")
    public Response getNegotiation(@PathParam("id") String id, @HeaderParam(AUTHORIZATION) String token) {
        return errorResponse(Optional.of(id), Response.Status.NOT_IMPLEMENTED, NOT_IMPLEMENTED);
    }

    /**
     * Provider-specific endpoint.
     *
     * @param jsonObject dspace:ContractRequestMessage sent by a consumer.
     * @param token      identity token.
     * @return the created contract negotiation or an error.
     */
    @POST
    @Path(INITIAL_CONTRACT_REQUEST)
    public Response initiateNegotiation(JsonObject jsonObject,
                                        @HeaderParam(AUTHORIZATION) String token) {
        var negotiationResult = handleMessage(MessageSpec.Builder.newInstance(ContractRequestMessage.class)
                .expectedMessageType(DSPACE_TYPE_CONTRACT_REQUEST_MESSAGE)
                .message(jsonObject)
                .token(token)
                .serviceCall(this::validateAndProcessRequest)
                .build());

        if (negotiationResult.failed()) {
            return errorResponse(Optional.empty(), getHttpStatus(negotiationResult.reason()), negotiationResult.getFailureDetail());
        }

        var negotiation = negotiationResult.getContent();
        return transformerRegistry.transform(negotiation, JsonObject.class)
                .map(transformedJson -> Response.ok().type(MediaType.APPLICATION_JSON).entity(transformedJson).build())
                .orElse(failure -> {
                    var errorCode = UUID.randomUUID();
                    monitor.warning(String.format("Error transforming negotiation, error id %s: %s", errorCode, failure.getFailureDetail()));
                    return errorResponse(Optional.of(negotiation.getCorrelationId()), Response.Status.INTERNAL_SERVER_ERROR, String.format("Error code %s", errorCode));
                });

    }

    /**
     * Provider-specific endpoint.
     *
     * @param id         of contract negotiation.
     * @param jsonObject dspace:ContractRequestMessage sent by a consumer.
     * @param token      identity token.
     * @return the created contract negotiation or an error.
     */
    @POST
    @Path("{id}" + CONTRACT_REQUEST)
    public Response consumerOffer(@PathParam("id") String id,
                                  JsonObject jsonObject,
                                  @HeaderParam(AUTHORIZATION) String token) {
        return handleMessage(MessageSpec.Builder.newInstance(ContractRequestMessage.class)
                .expectedMessageType(DSPACE_TYPE_CONTRACT_REQUEST_MESSAGE)
                .processId(id)
                .message(jsonObject)
                .token(token)
                .serviceCall(protocolService::notifyRequested)
                .build())
                .map(cn -> Response.ok().build())
                .orElse(createErrorResponse(id));
    }

    /**
     * Endpoint on provider and consumer side.
     *
     * @param id         of contract negotiation.
     * @param jsonObject dspace:ContractNegotiationEventMessage sent by consumer or provider.
     * @param token      identity token.
     * @return empty response or error.
     */
    @POST
    @Path("{id}" + EVENT)
    public Response createEvent(@PathParam("id") String id,
                                JsonObject jsonObject,
                                @HeaderParam(AUTHORIZATION) String token) {
        return handleMessage(MessageSpec.Builder.newInstance(ContractNegotiationEventMessage.class)
                .expectedMessageType(DSPACE_TYPE_CONTRACT_NEGOTIATION_EVENT_MESSAGE)
                .processId(id)
                .message(jsonObject)
                .token(token)
                .serviceCall(this::dispatchEventMessage)
                .build())
                .map(cn -> Response.ok().build())
                .orElse(createErrorResponse(id));
    }

    /**
     * Provider-specific endpoint.
     *
     * @param id         of contract negotiation.
     * @param jsonObject dspace:ContractAgreementVerificationMessage sent by a consumer.
     * @param token      identity token.
     * @return empty response or error.
     */
    @POST
    @Path("{id}" + AGREEMENT + VERIFICATION)
    public Response verifyAgreement(@PathParam("id") String id,
                                    JsonObject jsonObject,
                                    @HeaderParam(AUTHORIZATION) String token) {
        return handleMessage(MessageSpec.Builder.newInstance(ContractAgreementVerificationMessage.class)
                .expectedMessageType(DSPACE_TYPE_CONTRACT_AGREEMENT_VERIFICATION_MESSAGE)
                .processId(id)
                .message(jsonObject)
                .token(token)
                .serviceCall(protocolService::notifyVerified)
                .build())
                .map(cn -> Response.ok().build())
                .orElse(createErrorResponse(id));
    }

    /**
     * Endpoint on provider and consumer side.
     *
     * @param id         of contract negotiation.
     * @param jsonObject dspace:ContractNegotiationTerminationMessage sent by consumer or provider.
     * @param token      identity token.
     * @return empty response or error.
     */
    @POST
    @Path("{id}" + TERMINATION)
    public Response terminateNegotiation(@PathParam("id") String id,
                                         JsonObject jsonObject,
                                         @HeaderParam(AUTHORIZATION) String token) {
        return handleMessage(MessageSpec.Builder.newInstance(ContractNegotiationTerminationMessage.class)
                .expectedMessageType(DSPACE_TYPE_CONTRACT_NEGOTIATION_TERMINATION_MESSAGE)
                .processId(id)
                .message(jsonObject)
                .token(token)
                .serviceCall(protocolService::notifyTerminated)
                .build())
                .map(cn -> Response.ok().build())
                .orElse(createErrorResponse(id));
    }

    /**
     * Consumer-specific endpoint.
     *
     * @param id    of contract negotiation.
     * @param body  dspace:ContractOfferMessage sent by a provider.
     * @param token identity token.
     * @return empty response or error.
     */
    @POST
    @Path("{id}" + CONTRACT_OFFER)
    public Response providerOffer(@PathParam("id") String id,
                                  JsonObject body,
                                  @HeaderParam(AUTHORIZATION) String token) {
        return errorResponse(Optional.of(id), Response.Status.NOT_IMPLEMENTED, NOT_IMPLEMENTED);
    }

    /**
     * Consumer-specific endpoint.
     *
     * @param id         of contract negotiation.
     * @param jsonObject dspace:ContractAgreementMessage sent by a provider.
     * @param token      identity token.
     * @return empty response or error.
     */
    @POST
    @Path("{id}" + AGREEMENT)
    public Response createAgreement(@PathParam("id") String id,
                                    JsonObject jsonObject,
                                    @HeaderParam(AUTHORIZATION) String token) {
        return handleMessage(MessageSpec.Builder.newInstance(ContractAgreementMessage.class)
                .expectedMessageType(DSPACE_TYPE_CONTRACT_AGREEMENT_MESSAGE)
                .processId(id)
                .message(jsonObject)
                .token(token)
                .serviceCall(protocolService::notifyAgreed)
                .build())
                .map(cn -> Response.ok().build())
                .orElse(createErrorResponse(id));
    }

    private <M extends ContractRemoteMessage> ServiceResult<ContractNegotiation> handleMessage(MessageSpec<M> messageSpec) {
        monitor.debug(() -> format("DSP: Incoming %s for contract negotiation process%s",
                messageSpec.getMessageClass().getSimpleName(), messageSpec.getProcessId() != null ? ": " + messageSpec.getProcessId() : ""));

        var claimToken = checkAndReturnAuthToken(messageSpec.getToken());
        if (claimToken.failed()) {
            return ServiceResult.unauthorized(claimToken.getFailureMessages());
        }

        var expanded = jsonLdService.expand(messageSpec.getMessage());
        if (expanded.failed()) {
            return ServiceResult.badRequest(expanded.getFailureMessages());
        }

        if (!isOfExpectedType(expanded.getContent(), messageSpec.getExpectedMessageType())) {
            return ServiceResult.badRequest(format("Request body was not of expected type: %s", messageSpec.getExpectedMessageType()));
        }

        var ingressResult = transformerRegistry.transform(expanded.getContent(), messageSpec.getMessageClass())
                .compose(contractNegotiationMessage -> {
                    // set the remote protocol used
                    contractNegotiationMessage.setProtocol(DATASPACE_PROTOCOL_HTTP);
                    return validateProcessId(messageSpec.getProcessId(), contractNegotiationMessage);
                });

        if (ingressResult.failed()) {
            return ServiceResult.badRequest(format("Failed to read request body: %s", ingressResult.getFailureDetail()));
        }


        // invokes negotiation protocol service method
        return messageSpec.getServiceCall().apply(ingressResult.getContent(), claimToken.getContent());
    }

    @NotNull
    private ServiceResult<ContractNegotiation> validateAndProcessRequest(ContractRequestMessage message, ClaimToken claimToken) {
        if (message.getCallbackAddress() == null) {
            throw new InvalidRequestException(format("ContractRequestMessage must contain a '%s' property", DSPACE_PROPERTY_CALLBACK_ADDRESS));
        }
        return protocolService.notifyRequested(message, claimToken);
    }

    @NotNull
    private ServiceResult<ContractNegotiation> dispatchEventMessage(ContractNegotiationEventMessage message, ClaimToken claimToken) {
        switch (message.getType()) {
            case FINALIZED:
                return protocolService.notifyFinalized(message, claimToken);
            case ACCEPTED:
                return protocolService.notifyAccepted(message, claimToken);
            default:
                throw new InvalidRequestException(String.format("Cannot process dspace:ContractNegotiationEventMessage with unexpected type %s.", message.getType()));
        }
    }

    private Result<ClaimToken> checkAndReturnAuthToken(String token) {
        var tokenRepresentation = TokenRepresentation.Builder.newInstance().token(token).build();
        return identityService.verifyJwtToken(tokenRepresentation, callbackAddress);
    }

    private <M extends ContractRemoteMessage> Result<M> validateProcessId(@Nullable String expected, M actual) {

        if (expected == null) {
            return Result.success(actual);
        }
        return Objects.equals(expected, actual.getProcessId()) ? Result.success(actual) : Result.failure(format("Invalid process ID. Expected: %s, actual: %s", expected, actual));
    }

    private Response errorResponse(Optional<String> processId, Response.Status code, String message) {
        var builder = DspError.Builder.newInstance()
                .type(DSPACE_TYPE_CONTRACT_NEGOTIATION_ERROR)
                .code(Integer.toString(code.getStatusCode()))
                .messages(List.of(message));

        processId.ifPresent(builder::processId);

        return Response.status(code).type(MediaType.APPLICATION_JSON)
                .entity(builder.build().toJson())
                .build();
    }

    @NotNull
    private Response.Status getHttpStatus(ServiceFailure.Reason reason) {
        switch (reason) {
            case UNAUTHORIZED:
                return Response.Status.UNAUTHORIZED;
            case CONFLICT:
                return Response.Status.CONFLICT;
            default:
                return Response.Status.BAD_REQUEST;
        }

    }

    @NotNull
    private Function<ServiceFailure, Response> createErrorResponse(String id) {
        return f -> errorResponse(Optional.of(id), getHttpStatus(f.getReason()), f.getFailureDetail());
    }

}
