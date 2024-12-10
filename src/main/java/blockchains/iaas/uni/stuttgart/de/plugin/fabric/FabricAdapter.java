/*******************************************************************************
 * Copyright (c) 2019-2024 Institute for the Architecture of Application System - University of Stuttgart
 * Author: Ghareeb Falazi
 *
 * This program and the accompanying materials are made available under the
 * terms the Apache Software License 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: Apache-2.0
 *******************************************************************************/

package blockchains.iaas.uni.stuttgart.de.plugin.fabric;

import blockchains.iaas.uni.stuttgart.de.api.exceptions.*;
import blockchains.iaas.uni.stuttgart.de.api.interfaces.BlockchainAdapter;
import blockchains.iaas.uni.stuttgart.de.api.model.Transaction;
import blockchains.iaas.uni.stuttgart.de.api.model.*;
import blockchains.iaas.uni.stuttgart.de.api.utils.BooleanExpressionEvaluator;
import blockchains.iaas.uni.stuttgart.de.api.utils.SmartContractPathParser;
import blockchains.iaas.uni.stuttgart.de.plugin.fabric.utils.AsyncManager;
import com.google.gson.*;
import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.Status;
import io.grpc.*;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.hyperledger.fabric.client.*;
import org.hyperledger.fabric.client.identity.*;
import org.hyperledger.fabric.protos.common.BlockchainInfo;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.cert.CertificateException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Log4j2
public class FabricAdapter implements BlockchainAdapter {
    private static final int EVALUATION_TIMEOUT_SECONDS = 5;
    private static final int ENDORSEMENT_TIMEOUT_SECONDS = 15;
    private static final int SUBMISSION_TIMEOUT_SECONDS = 5;
    private static final int COMMITMENT_TIMEOUT_SECONDS = 60;
    private static final int EVENT_QUERY_TIMEOUT_SECONDS = 5;
    private final Path certDirPath;
    private final Path keyDirPath;
    private final Path tlsCertPath;
    private final String mspId;
    private final String peerEndpoint;
    private final String overrideAuth;
    private final String resourceManagerSmartContractAddress;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public FabricAdapter(final String userName, final String cryptoPath,
                         final String mspId,
                         final String peerEndpoint,
                         final String overrideAuth,
                         final String resourceManagerSmartContractAddress) {
        this.resourceManagerSmartContractAddress = resourceManagerSmartContractAddress;
        this.mspId = mspId;
        this.peerEndpoint = peerEndpoint;
        this.overrideAuth = overrideAuth;
        final Path CRYPTO_PATH = Paths.get(cryptoPath);
        String peerAddress = overrideAuth != null && !overrideAuth.isEmpty() ? overrideAuth : peerEndpoint;
        String orgName = Stream.of(peerAddress.split("\\.")).skip(1).collect(Collectors.joining("."));
        String userFolder = userName + "@" + orgName;
        this.keyDirPath = CRYPTO_PATH.resolve("users").resolve(userFolder).resolve("msp").resolve("keystore");
        this.certDirPath = CRYPTO_PATH.resolve("users").resolve(userFolder).resolve("msp").resolve("signcerts");
        this.tlsCertPath = CRYPTO_PATH.resolve("peers").resolve(peerAddress).resolve("tls").resolve("ca.crt");
    }


    static Path getFirstFilePath(Path dirPath) throws IOException {
        try (var keyFiles = Files.list(dirPath)) {
            return keyFiles.findFirst().orElseThrow();
        }
    }

    public static LocalDateTime getCurrentTimestamp() {
        Instant now = Instant.now();
        return now.atZone(ZoneId.of("UTC")).toLocalDateTime();
    }

    protected Gateway createGateway(Channel channel) throws IOException, CertificateException, InvalidKeyException {
        var builder = Gateway.newInstance().identity(newIdentity()).signer(newSigner()).connection(channel)
                // Default timeouts for different gRPC calls
                .evaluateOptions(options -> options.withDeadlineAfter(EVALUATION_TIMEOUT_SECONDS, TimeUnit.SECONDS))
                .endorseOptions(options -> options.withDeadlineAfter(ENDORSEMENT_TIMEOUT_SECONDS, TimeUnit.SECONDS))
                .submitOptions(options -> options.withDeadlineAfter(SUBMISSION_TIMEOUT_SECONDS, TimeUnit.SECONDS))
                .commitStatusOptions(options -> options.withDeadlineAfter(COMMITMENT_TIMEOUT_SECONDS, TimeUnit.SECONDS));

        return builder.connect();
    }

    protected ManagedChannel newGrpcConnection() throws IOException {
        var credentials = TlsChannelCredentials.newBuilder()
                .trustManager(this.tlsCertPath.toFile())
                .build();
        return Grpc.newChannelBuilder(this.peerEndpoint, credentials)
                .overrideAuthority(this.overrideAuth)
                .build();
    }

    protected X509Identity newIdentity() throws IOException, CertificateException {
        try (var certReader = Files.newBufferedReader(getFirstFilePath(this.certDirPath))) {
            var certificate = Identities.readX509Certificate(certReader);
            return new X509Identity(this.mspId, certificate);
        }
    }

    protected Signer newSigner() throws IOException, InvalidKeyException {
        try (var keyReader = Files.newBufferedReader(getFirstFilePath(this.keyDirPath))) {
            var privateKey = Identities.readPrivateKey(keyReader);
            return Signers.newPrivateKeySigner(privateKey);
        }
    }

    @Override
    public CompletableFuture<Transaction> submitTransaction(String receiverAddress, BigDecimal value, double requiredConfidence

    ) throws InvalidTransactionException, NotSupportedException {
        throw new NotSupportedException("Fabric does not support submitting monetary transactions!");
    }

    @Override
    public Observable<Transaction> receiveTransactions(String senderId, double requiredConfidence) throws NotSupportedException {
        throw new NotSupportedException("Fabric does not support receiving monetary transactions!");
    }

    @Override
    public CompletableFuture<TransactionState> ensureTransactionState(String transactionId, double requiredConfidence) throws NotSupportedException {
        throw new NotSupportedException("Fabric does not support monetary transactions!");
    }

    @Override
    public CompletableFuture<TransactionState> detectOrphanedTransaction(String transactionId) throws NotSupportedException {
        throw new NotSupportedException("Fabric does not support monetary transactions!");
    }

    @Override
    public CompletableFuture<Transaction> invokeSmartContract(
            String smartContractPath,
            String functionIdentifier,
            List<Parameter> inputs,
            List<Parameter> outputs,
            double requiredConfidence,
            long timeoutMillis,
            boolean sideEffects) throws BalException {

        if (outputs.size() > 1) {
            throw new ParameterException("Hyperledger Fabric supports only at most a single return value.");
        }

        CompletableFuture<Transaction> result = new CompletableFuture<>();
        SmartContractPathElements path = this.parsePathElements(smartContractPath);

        try {
            ManagedChannel channel = newGrpcConnection();

            try (Gateway gateway = createGateway(channel)) {
                Network network = gateway.getNetwork(path.channel);
                Contract contract = path.smartContract != null && !path.smartContract.isEmpty() ?
                        network.getContract(path.chaincode, path.smartContract) :
                        network.getContract(path.chaincode);
                String[] params = inputs.stream().map(Parameter::getValue).toArray(String[]::new);

                try {
                    byte[] resultAsBytes;

                    if (sideEffects) {
                        resultAsBytes = contract.submitTransaction(functionIdentifier, params);
                    } else {
                        resultAsBytes = contract.evaluateTransaction(functionIdentifier, params);
                    }

                    Transaction resultT = new Transaction();

                    if (outputs.size() == 1) {
                        Parameter resultP = Parameter
                                .builder()
                                .name(outputs.get(0).getName())
                                .type(outputs.get(0).getType())
                                .value(new String(resultAsBytes, StandardCharsets.UTF_8))
                                .build();
                        resultT.setReturnValues(Collections.singletonList(resultP));
                        log.info(resultP.getValue());
                    } else if (outputs.isEmpty()) {
                        log.info("Fabric transaction without a return value executed!");
                        resultT.setReturnValues(Collections.emptyList());
                    }

                    resultT.setState(TransactionState.RETURN_VALUE);
                    result.complete(resultT);
                } catch (Exception e) {
                    log.error("Failed to invoke smart contract function {}/{}.", smartContractPath, functionIdentifier, e);
                    // exceptions at this level are invocation exceptions. They should be sent asynchronously to the client app.
                    result.completeExceptionally(new InvokeSmartContractFunctionFailure(e.getMessage()));
                }
            } catch (Exception e) {
                log.error("Failed to invoke smart contract function {}/{}.", smartContractPath, functionIdentifier, e);
                // this is a synchronous exception.
                throw new BlockchainNodeUnreachableException(e.getMessage());
            } finally {
                channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
            }
        } catch (IOException e) {
            log.error("Failed to establish network connection.", e);
            throw new BlockchainNodeUnreachableException(e.getMessage());
        } catch (InterruptedException e) {
            log.warn("An error occurred while trying to close the network connection. Ignoring...", e);
        }

        return result;
    }

    @Override
    public Observable<Occurrence> subscribeToEvent(
            String smartContractAddress,
            String eventIdentifier,
            List<Parameter> outputParameters,
            double degreeOfConfidence,
            String filter) throws BalException {
        SmartContractPathElements path = this.parsePathElements(smartContractAddress);
        final PublishSubject<Occurrence> result = PublishSubject.create();
        CloseableIterator<ChaincodeEvent> eventIter;
        ExecutorService executorService = AsyncManager.createExecutorService();
        Gateway gateway;

        try {
            ManagedChannel channel = newGrpcConnection();

            try {
                gateway = createGateway(channel);
                Network network = gateway.getNetwork(path.channel);
                eventIter = network.getChaincodeEvents(path.chaincode);
                executorService.execute(() -> {
                    try {
                        eventIter.forEachRemaining(event -> {

                            log.debug("Received chaincode event: {}", event);

                            try {
                                Occurrence occurrence = this.handleEvent(event, eventIdentifier, outputParameters, filter);

                                if (occurrence != null) {
                                    result.onNext(occurrence);
                                }
                            } catch (InvalidScipParameterException e) {
                                log.error("An error occurred while handling chaincode event: {}", event, e);
                                result.onError(e);
                            }
                        });
                    } catch (GatewayRuntimeException e) {
                        if (e.getStatus().getCode() != Status.Code.CANCELLED) {
                            throw e;
                        }
                    }
                });

            } catch (CertificateException | InvalidKeyException e) {
                log.error("Failed to subscribe to event {}/{}.", smartContractAddress, eventIdentifier);
                // this is a synchronous exception.
                throw new BlockchainNodeUnreachableException(e.getMessage());
            }

            return result.doFinally(() -> {
                if (eventIter != null) {
                    eventIter.close();
                }

                try {
                    channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    log.warn("An error occurred while trying to close the network connection. Ignoring...", e);
                }

                gateway.close();

                executorService.shutdownNow();
            });

        } catch (IOException e) {
            log.error("Failed to establish network connection.", e);
            throw new BlockchainNodeUnreachableException(e.getMessage());
        }

    }

    @Override
    public CompletableFuture<QueryResult> queryEvents(String smartContractAddress, String eventIdentifier, List<Parameter> outputParameters, String filter, TimeFrame timeFrame) throws BalException {

        SmartContractPathElements path = this.parsePathElements(smartContractAddress);
        // todo find a way to read date time from block numbers
        final LocalDateTime fromDateTime = timeFrame != null ? timeFrame.getFromLocalDateTime() : null;
        final LocalDateTime toDateTime = timeFrame != null ? timeFrame.getToLocalDateTime() : null;


        try {
            ManagedChannel channel = newGrpcConnection();
            final CompletableFuture<QueryResult> result = new CompletableFuture<>();
            final QueryResult queryResult = QueryResult.builder().occurrences(new ArrayList<>()).build();

            try (Gateway gateway = createGateway(channel)) {
                Network network = gateway.getNetwork(path.channel);
                final long currentBlockNumber = getCurrentBlockHeight(network, path.channel);
                var request = network.newChaincodeEventsRequest(path.chaincode)
                        .startBlock(0)
                        .build();

                try (var eventIter = request.getEvents(callOptions -> callOptions.withDeadlineAfter(EVENT_QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS))) {
                    while (eventIter.hasNext()) {
                        ChaincodeEvent event = eventIter.next();
                        log.debug("Handling event: {}...", event);
                        Occurrence currentOccurrence = handleEvent(event, eventIdentifier, outputParameters, filter);

                        if (currentOccurrence != null) {
                            queryResult.getOccurrences().add(currentOccurrence);
                        }
                        if (event.getBlockNumber() >= currentBlockNumber) {
                            break;
                        }
                    }

                    result.complete(queryResult);
                }

            } catch (CertificateException | InvalidKeyException | GatewayException e) {
                log.error("Failed to query past event occurrences for event: {}/{}.", smartContractAddress, eventIdentifier);
                // this is a synchronous exception.
                throw new BlockchainNodeUnreachableException(e.getMessage());
            } catch(GatewayRuntimeException e) {
                // hacky way to finish waiting for events!
               if (e.getStatus().getCode() == Status.DEADLINE_EXCEEDED.getCode()) {
                   result.complete(queryResult);
               } else {
                   throw e;
               }
            } finally {
                try {
                    channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    log.warn("An error occurred while trying to close the network connection. Ignoring...", e);
                }
            }

            return result;

        } catch (IOException e) {
            log.error("Failed to establish network connection.", e);
            throw new BlockchainNodeUnreachableException(e.getMessage());
        }

    }

    @Override
    public ResourceManagerSmartContract getResourceManagerSmartContract() throws NotSupportedException {
        Parameter txId = new Parameter("txId",
                "{ \"type\": \"string\" }",
                null);
        List<Parameter> txIdAsList = new ArrayList<>();
        List<Parameter> emptyList = new ArrayList<>();
        txIdAsList.add(txId);
        SmartContractFunction prepare = new SmartContractFunction("prepare", txIdAsList, emptyList);
        SmartContractFunction commit = new SmartContractFunction("commit", txIdAsList, emptyList);
        SmartContractFunction abort = new SmartContractFunction("abort", txIdAsList, emptyList);
        Parameter owner = new Parameter("owner",
                "{ \"type\": \"string\" }",
                null);
        Parameter isYes = new Parameter("isYes",
                "{ \"type\": \"string\" }",
                null);
        List<Parameter> votedEventParams = new ArrayList<>();
        votedEventParams.add(owner);
        votedEventParams.add(txId);
        votedEventParams.add(isYes);
        List<Parameter> abortedEventParams = new ArrayList<>();
        abortedEventParams.add(owner);
        abortedEventParams.add(txId);
        SmartContractEvent voted = new SmartContractEvent("Voted", votedEventParams);
        SmartContractEvent aborted = new SmartContractEvent("TxAborted", abortedEventParams);
        List<SmartContractFunction> functions = new ArrayList<>();
        functions.add(prepare);
        functions.add(commit);
        functions.add(abort);
        List<SmartContractEvent> events = new ArrayList<>();
        events.add(voted);
        events.add(aborted);

        return new FabricResourceManagerSmartContract(this.resourceManagerSmartContractAddress, functions, events);
    }

    private String prettyJson(final byte[] json) {
        return prettyJson(new String(json, StandardCharsets.UTF_8));
    }

    private String prettyJson(final String json) {
        var parsedJson = JsonParser.parseString(json);
        return gson.toJson(parsedJson);
    }

    private Occurrence handleEvent(ChaincodeEvent event, String eventName, List<Parameter> outputParameters, String filter) throws InvalidScipParameterException {
        // todo try to parse the returned value according to the outputParameters
        if (!event.getEventName().equalsIgnoreCase(eventName)) {
            return null;
        }

        JsonObject json = JsonParser.parseString(prettyJson(event.getPayload())).getAsJsonObject();
        outputParameters = outputParameters == null? new ArrayList<>() : outputParameters;

        for (Parameter parameter : outputParameters) {
            String value = json.get(parameter.getName()).getAsString();
            parameter.setValue(value);
        }

        try {
            if (BooleanExpressionEvaluator.evaluate(filter, outputParameters)) {
                return Occurrence
                        .builder()
                        .parameters(outputParameters)
                        .isoTimestamp(DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneId.of("UTC")).format(getCurrentTimestamp()))
                        .build();
            }

            return null;
        } catch (Exception e) {
            throw new InvalidScipParameterException(e.getMessage());
        }
    }

    protected long getCurrentBlockHeight(Network network, String channelName) throws GatewayException, InvalidProtocolBufferException {
        byte[] rawResult = network.getContract("qscc").evaluateTransaction("GetChainInfo", channelName);
        return BlockchainInfo.parseFrom(rawResult).getHeight();
    }

    @Override
    public String testConnection() {
        try {
            ManagedChannel channel = newGrpcConnection();
            ConnectivityState state = channel.getState(true);
            channel.shutdownNow();

            return state.toString();

        } catch (Exception e) {
            log.error("Failed to establish network connection.", e);
            return e.getMessage();
        }
    }

    private SmartContractPathElements parsePathElements(String smartContractPath) throws InvokeSmartContractFunctionFailure {
        SmartContractPathParser parser = SmartContractPathParser.parse(smartContractPath);
        String[] pathSegments = parser.getSmartContractPathSegments();

        if (pathSegments.length != 3 && pathSegments.length != 2) {
            String message = String.format("Unable to identify the path to the requested function. Expected path segments: 3 or 2. Found path segments: %s", pathSegments.length);
            log.error(message);
            throw new InvalidScipParameterException(message);
        }

        SmartContractPathElements.SmartContractPathElementsBuilder builder = SmartContractPathElements
                .builder()
                .channel(pathSegments[0])
                .chaincode(pathSegments[1]);

        if (pathSegments.length == 3) {
            builder = builder.smartContract(pathSegments[2]);
        }

        return builder.build();
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    private static class SmartContractPathElements {
        private String channel;
        private String chaincode;
        private String smartContract;
    }
}
