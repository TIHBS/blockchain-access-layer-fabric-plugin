package blockchains.iaas.uni.stuttgart.de.plugin.fabric;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FabricConnectionProfileTest {

    @Test
    void getIdentity() {
        FabricConnectionProfile profile = new FabricConnectionProfile();
        profile.setCryptoPath("C:\\Users\\Ghareeb\\Documents\\GitHub\\TIHBS\\tccsci-demo\\fabric\\fabric-samples\\test-network\\organizations\\peerOrganizations\\org1.example.com");
        profile.setUsername("User1");
        profile.setMspId("Org1MSP");
        profile.setOverrideAuth("peer0.org1.example.com");
        profile.setPeerEndpoint("localhost:7051");
        Assertions.assertEquals("CN=user1,OU=client,O=Hyperledger,ST=North Carolina,C=US", profile.getIdentity());
    }
}