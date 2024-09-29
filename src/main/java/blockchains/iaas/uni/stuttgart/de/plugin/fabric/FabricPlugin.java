/*******************************************************************************
 * Copyright (c) 2022-2024 Institute for the Architecture of Application System - University of Stuttgart
 * Author: Akshay Patel
 * Co-Author: Ghareeb Falazi
 *
 * This program and the accompanying materials are made available under the
 * terms the Apache Software License 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: Apache-2.0
 *******************************************************************************/

package blockchains.iaas.uni.stuttgart.de.plugin.fabric;

import blockchains.iaas.uni.stuttgart.de.api.IAdapterExtension;
import blockchains.iaas.uni.stuttgart.de.api.connectionprofiles.AbstractConnectionProfile;
import org.pf4j.Extension;
import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

public class FabricPlugin extends Plugin {
    /**
     * Constructor to be used by plugin manager for plugin instantiation.
     * Your plugins have to provide constructor with this exact signature to
     * be successfully loaded by manager.
     *
     * @param wrapper
     */
    public FabricPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void start() {
        super.start();
    }

    @Override
    public void stop() {
        super.stop();
    }

    @Extension
    public static class FabricAdapterImpl implements IAdapterExtension {

        @Override
        public FabricAdapter getAdapter(AbstractConnectionProfile connectionProfile) {
            assert connectionProfile instanceof FabricConnectionProfile;
            FabricConnectionProfile fabricConnectionProfile = (FabricConnectionProfile) connectionProfile;

            return new FabricAdapter(
                    fabricConnectionProfile.getUsername(),
                    fabricConnectionProfile.getCryptoPath(),
                    fabricConnectionProfile.getMspId(),
                    fabricConnectionProfile.getPeerEndpoint(),
                    fabricConnectionProfile.getOverrideAuth(),
                    fabricConnectionProfile.getResourceManagerSmartContractAddress());

        }

        @Override
        public Class<? extends AbstractConnectionProfile> getConnectionProfileClass() {
            return FabricConnectionProfile.class;
        }

        @Override
        public String getConnectionProfileNamedType() {
            return "fabric";
        }

        @Override
        public String getBlockChainId() {
            return "fabric";
        }

    }
}
