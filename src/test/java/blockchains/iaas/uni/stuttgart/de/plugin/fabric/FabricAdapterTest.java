package blockchains.iaas.uni.stuttgart.de.plugin.fabric;

import blockchains.iaas.uni.stuttgart.de.api.model.*;
import io.grpc.ManagedChannel;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import lombok.extern.log4j.Log4j2;
import org.hyperledger.fabric.client.Gateway;
import org.hyperledger.fabric.client.GatewayException;
import org.hyperledger.fabric.client.Network;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.cert.CertificateException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * These tests can only be run with an existing fabric network with a suitable chaincode installed.
 * Therefore, ensure to run <a href="https://hyperledger-fabric.readthedocs.io/en/latest/write_first_app.html">the tutorial</a>
 * Choose the folder 'fabric-samples\asset-transfer-events' for installing the chaincode (it does not matter which language
 */
@Log4j2
class FabricAdapterTest {
    final static String stringType = "{ \"type\": \"string\" }";
    final String smartContractPath = "mychannel/events";
    final String functionName = "CreateAsset";

    @Test
    void testInvokeSmartContract() throws ExecutionException, InterruptedException {
        FabricAdapter adapter = getAdapter();
        Transaction tx = invokeSmartContract(adapter);
        assertNotNull(tx);
        assertEquals(TransactionState.RETURN_VALUE, tx.getState());
        assertEquals(1, tx.getReturnValues().size());
    }

    @Test
    void testSubscribeEvent() throws ExecutionException, InterruptedException {
        List<Occurrence> occurrences = new ArrayList<>();
        FabricAdapter adapter = getAdapter();
        Observable<Occurrence> obs = adapter.subscribeToEvent(smartContractPath, "CreateAsset", List.of(new Parameter("EventData", stringType, null)), 1.0, null);
        Disposable dis = obs.subscribe(occurrences::add);
        invokeSmartContract(adapter);
        invokeSmartContract(adapter);
        Thread.sleep(15 * 1000);
        dis.dispose();
        assertEquals(2, occurrences.size());
    }

    @Test
    void testQueryEvents() throws ExecutionException, InterruptedException {
        FabricAdapter adapter = getAdapter();
        LocalDateTime now = LocalDateTime.now();
        invokeSmartContract(adapter, now.toString());
        invokeSmartContract(adapter, now.toString());
        QueryResult result = adapter.queryEvents(smartContractPath, "CreateAsset", List.of(new Parameter("EventData", stringType, null)), "", null).get();
        List<Occurrence> occurrences = result.getOccurrences();
        assertNotNull(occurrences);
        assertTrue(occurrences.size() >= 2);
        assertEquals(2, occurrences.stream().map(o -> o.getParameters().get(0).getValue()).filter(t -> t.contains(now.toString())).count());
    }
    @Test
    void testGetCurrentBlockHeight() throws ExecutionException, InterruptedException, IOException, CertificateException, InvalidKeyException, GatewayException {
        FabricAdapter adapter = getAdapter();
        invokeSmartContract(adapter);
        ManagedChannel channel = adapter.newGrpcConnection();

        try (Gateway gateway = adapter.createGateway(channel)) {
            Network network = gateway.getNetwork("mychannel");
            long height = adapter.getCurrentBlockHeight(network, "mychannel");
            assertTrue(height > 0);
        }

        channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }

    private Transaction invokeSmartContract(FabricAdapter adapter) throws ExecutionException, InterruptedException {
        return invokeSmartContract(adapter, "20");
    }

    private Transaction invokeSmartContract(FabricAdapter adapter, String size) throws ExecutionException, InterruptedException {
        List<Parameter> parameters = getCreateAssetParameters(size);
        return adapter.invokeSmartContract(smartContractPath, functionName, parameters, List.of(new Parameter("Result", stringType, null)), 1.0, 0).get();
    }


    private List<Parameter> getCreateAssetParameters(String size) {
        return List.of(
                new Parameter("ID", stringType, "asset_" + Instant.now().getEpochSecond()),
                new Parameter("Color", stringType, "blue"),
                new Parameter("Size", stringType, size),
                new Parameter("Owner", stringType, "Ghareeb"),
                new Parameter("AppraisedValue", stringType, "100"));
    }

    private FabricAdapter getAdapter() {
        return new FabricAdapter("User1",
                "C:\\Users\\Ghareeb\\Documents\\GitHub\\Fabric\\fabric-samples\\test-network\\organizations\\peerOrganizations\\org1.example.com",
                "Org1MSP",
                "localhost:7051",
                "peer0.org1.example.com",
                "");
    }

}