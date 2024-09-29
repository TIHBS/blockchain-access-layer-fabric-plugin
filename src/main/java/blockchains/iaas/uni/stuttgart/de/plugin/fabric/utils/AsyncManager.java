/*******************************************************************************
 * Copyright (c) 2024 Institute for the Architecture of Application System - University of Stuttgart
 * Author: Ghareeb Falazi
 *
 * This program and the accompanying materials are made available under the
 * terms the Apache Software License 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: Apache-2.0
 *******************************************************************************/
package blockchains.iaas.uni.stuttgart.de.plugin.fabric.utils;

import lombok.extern.log4j.Log4j2;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Log4j2
public class AsyncManager {
    public static ExecutorService createExecutorService() {
        ExecutorService executor = Executors.newCachedThreadPool();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            shutdown(executor);
        }));
        return executor;
    }

    private static void shutdown(ExecutorService executorService) {
        executorService.shutdown();

        try {
            if (!executorService.awaitTermination(1, TimeUnit.MINUTES)) {
                executorService.shutdownNow();
                if (!executorService.awaitTermination(1, TimeUnit.MINUTES)) {
                    log.error("Failed to shut down the executor service!");
                }
            }
        } catch (InterruptedException e) {
            log.error("An error occurred while shutting down the executor service!", e);
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

    }
}
