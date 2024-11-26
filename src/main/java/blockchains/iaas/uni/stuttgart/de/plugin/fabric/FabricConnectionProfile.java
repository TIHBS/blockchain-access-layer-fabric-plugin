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
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.hyperledger.fabric.client.identity.Identities;
import org.hyperledger.fabric.client.identity.X509Identity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.CertificateException;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static blockchains.iaas.uni.stuttgart.de.plugin.fabric.FabricAdapter.getFirstFilePath;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Log4j2
@JsonTypeName("fabric")
public class FabricConnectionProfile extends AbstractConnectionProfile {
    private static final String PREFIX = "fabric.";
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
    public String getIdentity() {
        try {
            String orgName = Stream.of(overrideAuth.split("\\.")).skip(1).collect(Collectors.joining("."));
            String userFolder = username + "@" + orgName;
            Path certDirPath = Paths.get(cryptoPath).resolve("users").resolve(userFolder).resolve("msp").resolve("signcerts");
            try (var certReader = Files.newBufferedReader(getFirstFilePath(certDirPath))) {
                var certificate = Identities.readX509Certificate(certReader);
                return certificate.getSubjectX500Principal().getName();
            }
        } catch (IOException | CertificateException e) {
            log.error("Failed to read client identity", e);
            return null;
        }
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
