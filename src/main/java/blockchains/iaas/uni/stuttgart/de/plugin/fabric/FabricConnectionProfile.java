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

import blockchains.iaas.uni.stuttgart.de.api.connectionprofiles.AbstractConnectionProfile;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Properties;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class FabricConnectionProfile extends AbstractConnectionProfile {
    private static final String PREFIX = "hyperledger.fabric.";
    private static final String CRYPTO_PATH = PREFIX + "cryptoPath";
    private static final String MSP_ID = PREFIX + "mspId";
    private static final String USERNAME = PREFIX + "username";
    private static final String PEER_ENDPOINT = PREFIX + "peerEndpoint";
    private static final String OVERRIDE_AUTH = PREFIX + "overrideAuth";
    private static final String RMSC_ADDRESS = PREFIX + "rmscAddress";
    private String cryptoPath;
    private String mspId;
    private String peerEndpoint;
    private String overrideAuth;
    private String resourceManagerSmartContractAddress;
    private String username;


    @Override
    public Properties getAsProperties() {
        final Properties result = super.getAsProperties();
        result.setProperty(CRYPTO_PATH, this.cryptoPath);
        result.setProperty(MSP_ID, this.mspId);
        result.setProperty(PEER_ENDPOINT, this.peerEndpoint);
        result.setProperty(OVERRIDE_AUTH, this.overrideAuth);
        result.setProperty(RMSC_ADDRESS, this.resourceManagerSmartContractAddress);
        result.setProperty(USERNAME, this.username);

        return result;
    }

    @Override
    public Object getProperty(Object o) {
        assert o instanceof String;

        return switch (o.toString()) {
            case CRYPTO_PATH -> this.cryptoPath;
            case MSP_ID -> this.mspId;
            case PEER_ENDPOINT -> this.peerEndpoint;
            case OVERRIDE_AUTH -> this.overrideAuth;
            case RMSC_ADDRESS -> this.resourceManagerSmartContractAddress;
            case USERNAME -> this.username;
            default -> super.getAsProperties().get(o);
        };
    }

    @Override
    public void setProperty(Object o, Object o1) {
        assert o instanceof String;
        assert o1 instanceof String;

        Properties parent = super.getAsProperties();

        if (parent.containsKey(o)) {
            setAdversaryVotingRatio(Double.parseDouble(o1.toString()));
        } else {
            switch (o.toString()) {
                case CRYPTO_PATH -> this.cryptoPath = (String) o1;
                case MSP_ID -> this.mspId = (String) o1;
                case PEER_ENDPOINT -> this.peerEndpoint = (String) o1;
                case OVERRIDE_AUTH -> this.overrideAuth = (String) o1;
                case RMSC_ADDRESS -> this.resourceManagerSmartContractAddress = (String) o1;
                case USERNAME -> this.username = (String) o1;
            };
        }
    }
}
