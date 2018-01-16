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

import org.jgroups.JChannel;
import org.jgroups.util.Util;
import org.junit.Assert;
import org.junit.Test;

/**
 * Functional tests for AZURE_PING discovery.
 *
 * @author Radoslav Husar
 */
public class AZURE_PINGDiscoveryTest {

    public static final int CHANNEL_COUNT = 5;
    public static final String CLUSTER_NAME = UUID.randomUUID().toString();

    @Test
    public void testDiscovery() throws Exception {
        discover(CLUSTER_NAME);
    }

    @Test
    public void testDiscoveryObscureClusterName() throws Exception {
        String obscureClusterName = "``\\//--+ěščřžýáíé==''!@#$%^&*()_{}<>?";
        discover(obscureClusterName);
    }

    private void discover(String clusterName) throws Exception {
        List<JChannel> channels = create(clusterName);

        // Asserts the views are there
        for (JChannel channel : channels) {
            Assert.assertEquals("member count", CHANNEL_COUNT, channel.getView().getMembers().size());
        }

        printViews(channels);

        // Stop all channels
        // n.b. all channels must be closed, only disconnecting all concurrently can leave stale data
        for (JChannel channel : channels) {
            channel.close();
        }
    }

    private List<JChannel> create(String clusterName) throws Exception {
        List<JChannel> result = new LinkedList<>();
        for (int i = 0; i < CHANNEL_COUNT; i++) {
            JChannel channel = new JChannel(this.getClass().getResource("/org/jgroups/protocols/azure/tpc-azure.xml"));

            channel.connect(clusterName);
            if (i == 0) {
                // Lets be clear about the coordinator
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
