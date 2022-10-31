/*
 * Copyright 2015 Red Hat Inc., and individual contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jgroups.protocols.azure;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.jgroups.JChannel;
import org.jgroups.util.Util;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

/**
 * Functional tests for AZURE_PING discovery.
 *
 * @author Radoslav Husar
 */
public class AZURE_PINGDiscoveryTest {

    public static final String STACK_XML_CONFIGURATION = "org/jgroups/protocols/azure/tcp-azure.xml";
    public static final int CHANNEL_COUNT = 5;

    // The cluster names need to randomized so that multiple test runs can be run in parallel with the same
    // credentials (e.g. running JDK8 and JDK9 on the CI).
    public static final String RANDOM_CLUSTER_NAME = UUID.randomUUID().toString();

    /**
     * This is a simple smoke test to run with credentials are available. Verify that the test stack will fail to start
     * the channel in {@link AZURE_PING#validateConfiguration} throwing {@link IllegalArgumentException} and
     * not elsewhere, e.g. missing a protocol after a major jgroups.jar upgrade.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testProtocolStack() throws Exception {
        JChannel channel = new JChannel(STACK_XML_CONFIGURATION);

        channel.getProtocolStack().getProtocols().replaceAll(protocol -> {
            if (protocol instanceof AZURE_PING) {
                return new AZURE_PING();
            } else {
                return protocol;
            }
        });

        try {
            channel.connect(RANDOM_CLUSTER_NAME);
        } finally {
            channel.close();
        }

    }

    @Test
    public void testDiscovery() throws Exception {
        assumeCredentials();

        discover(RANDOM_CLUSTER_NAME);
    }

    @Test
    public void testDiscoveryObscureClusterName() throws Exception {
        assumeCredentials();

        String obscureClusterName = "``\\//--+ěščřžýáíé==''!@#$%^&*()_{}<>?";
        discover(obscureClusterName + RANDOM_CLUSTER_NAME);
    }

    private static void assumeCredentials() {
        Assume.assumeTrue("Credentials are not available, test is ignored!", System.getProperty("azure.access_key") != null && System.getProperty("azure.account_name") != null);
    }

    private void discover(String clusterName) throws Exception {
        List<JChannel> channels = create(clusterName);

        Thread.sleep(TimeUnit.SECONDS.toMillis(2));

        printViews(channels);

        // Asserts the views are there
        for (JChannel channel : channels) {
            Assert.assertEquals("member count", CHANNEL_COUNT, channel.getView().getMembers().size());
        }

        // Stop all channels
        // n.b. all channels must be closed, only disconnecting all concurrently can leave stale data
        for (JChannel channel : channels) {
            channel.close();
        }
    }

    private List<JChannel> create(String clusterName) throws Exception {
        List<JChannel> result = new LinkedList<>();
        for (int i = 0; i < CHANNEL_COUNT; i++) {
            JChannel channel = new JChannel(STACK_XML_CONFIGURATION);

            channel.connect(clusterName);
            if (i == 0) {
                // Let's be clear about the coordinator
                Util.sleep(1000);
            }
            result.add(channel);
        }
        return result;
    }

    protected static void printViews(List<JChannel> channels) {
        for (JChannel ch : channels) {
            System.out.println("Channel " + ch.getName() + " has view " + ch.getView());
        }
    }
}
