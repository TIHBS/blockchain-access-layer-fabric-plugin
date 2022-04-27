# blockchain-access-layer-fabric-plugin

### Setting-up a Basic Hyperledger Fabric Network

Please follow these steps [Fabric Setup](https://hyperledger-fabric.readthedocs.io/en/latest/getting_started.html)

#### Note

The included Fabric unit test depends on
the [FabCar official example](https://hyperledger-fabric.readthedocs.io/en/release-1.4/write_first_app.html), so in
order to run it ensure the following:

1. follow the steps of running the first Fabric tutorial
   at: https://hyperledger-fabric.readthedocs.io/en/release-1.4/write_first_app.html (use the javascript smart contract)
   .
2. execute the enrollAdmin.js and the registerUser.js node programs.
3. alter the local hosts file by adding the following entries:
    * 127.0.0.1 orderer.example.com
    * 127.0.0.1 peer0.org1.example.com
    * 127.0.0.1 peer0.org2.example.com
    * 127.0.0.1 peer1.org1.example.com
    * 127.0.0.1 peer1.org2.example.com

   This ensures that the SDK is able to find the orderer and network peers.
