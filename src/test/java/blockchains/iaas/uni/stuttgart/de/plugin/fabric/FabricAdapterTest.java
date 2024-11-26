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

import javax.security.auth.x500.X500Principal;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
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
 * Choose the folder 'fabric-samples\asset-transfer-events' for installing the chaincode (it does not matter which language)
 *
 * ./test-network/network.sh down
 * ./test-network/network.sh up createChannel -c mychannel -ca
 * ./test-network/network.sh deployCC -ccn basic -ccp ../asset-transfer-events/chaincode-javascript/ -ccl javascript
 */
@Log4j2
class FabricAdapterTest {
    final static String stringType = "{ \"type\": \"string\" }";
    final String smartContractPath = "mychannel/basic";
    final String functionName = "CreateAsset";

    @Test
    void testInvokeSmartContract() throws ExecutionException, InterruptedException, CertificateException, IOException {
        FabricAdapter adapter = getAdapter();
        final String id1 =  createUniqueId();
        Transaction tx = invokeCreateAsset(adapter, "6669", id1);
        assertNotNull(tx);
        assertEquals(TransactionState.RETURN_VALUE, tx.getState());
        assertEquals(1, tx.getReturnValues().size());
        tx = invokeGetAsset(adapter, id1);
        assertNotNull(tx);
        assertEquals(1, tx.getReturnValues().size());
        assertTrue(tx.getReturnValues().get(0).getValue().contains("6669"));
    }

    @Test
    void testSubscribeEvent() throws ExecutionException, InterruptedException, CertificateException, IOException {
        List<Occurrence> occurrences = new ArrayList<>();
        FabricAdapter adapter = getAdapter();
        Observable<Occurrence> obs = adapter.subscribeToEvent(smartContractPath, "CreateAsset", List.of(new Parameter("EventData", stringType, null)), 1.0, null);
        Disposable dis = obs.subscribe(occurrences::add);
        final String id1 =  createUniqueId();
        final String id2 = createUniqueId();
        invokeCreateAsset(adapter, "123456", id1);
        invokeCreateAsset(adapter, "987654", id2);
        Thread.sleep(15 * 1000);
        dis.dispose();
        assertEquals(2, occurrences.size());
    }

    @Test
    void testQueryEvents() throws ExecutionException, InterruptedException, CertificateException, IOException {
        FabricAdapter adapter = getAdapter();
        int num= (int)Math.ceil(Math.random()*1000.0);
        invokeCreateAsset(adapter, String.valueOf(num), createUniqueId());
        invokeCreateAsset(adapter, String.valueOf(num), createUniqueId());
        QueryResult result = adapter.queryEvents(smartContractPath, "CreateAsset", List.of(new Parameter("EventData", stringType, null)), "", null).get();
        List<Occurrence> occurrences = result.getOccurrences();
        assertNotNull(occurrences);
        assertTrue(occurrences.size() >= 2);
        assertTrue(2 <= occurrences.stream().map(o -> o.getParameters().get(0).getValue()).filter(t -> t.contains(String.valueOf(num))).count());
    }
    @Test
    void testGetCurrentBlockHeight() throws ExecutionException, InterruptedException, IOException, CertificateException, InvalidKeyException, GatewayException {
        FabricAdapter adapter = getAdapter();
        invokeCreateAsset(adapter, "20", "asset_" + Instant.now().getEpochSecond());
        ManagedChannel channel = adapter.newGrpcConnection();

        try (Gateway gateway = adapter.createGateway(channel)) {
            Network network = gateway.getNetwork("mychannel");
            long height = adapter.getCurrentBlockHeight(network, "mychannel");
            assertTrue(height > 0);
        }

        channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }


    private Transaction invokeCreateAsset(FabricAdapter adapter, String size, String id) throws ExecutionException, InterruptedException {
        List<Parameter> parameters = getCreateAssetParameters(size, id);
        return adapter.invokeSmartContract(smartContractPath, functionName, parameters, List.of(new Parameter("Result", stringType, null)), 1.0, 0, true).get();
    }

    private Transaction invokeGetAsset(FabricAdapter adapter, String id) throws ExecutionException, InterruptedException {
        List<Parameter> parameters = List.of(new Parameter("ID", stringType, id));
        return adapter.invokeSmartContract(smartContractPath, "ReadAsset", parameters, List.of(new Parameter("Result", stringType, null)), 1.0, 0, false).get();
    }


    private List<Parameter> getCreateAssetParameters(String size, String id) {
        return List.of(
                new Parameter("ID", stringType, id),
                new Parameter("Color", stringType, "blue"),
                new Parameter("Size", stringType, size),
                new Parameter("Owner", stringType, "Ghareeb"),
                new Parameter("AppraisedValue", stringType, "100"));
    }

    private FabricAdapter getAdapter() throws CertificateException, IOException {
        FabricAdapter adapter = new FabricAdapter("User1",
                "C:\\Users\\Ghareeb\\Documents\\GitHub\\TIHBS\\tccsci-demo\\fabric\\fabric-samples\\test-network\\organizations\\peerOrganizations\\org1.example.com",
                "Org1MSP",
                "localhost:7051",
                "peer0.org1.example.com",
                "");
        X500Principal cert = adapter.newIdentity().getCertificate().getSubjectX500Principal();
        log.info(cert);

        return adapter;
    }

    private static String createUniqueId() {
        return "asset_" + Instant.now().getEpochSecond();
    }

}