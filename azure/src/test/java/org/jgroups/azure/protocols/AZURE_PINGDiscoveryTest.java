package org.jgroups.azure.protocols;

import java.util.LinkedList;
import java.util.List;

import org.jgroups.JChannel;
import org.jgroups.util.Util;
import org.junit.Assert;
import org.junit.Test;

/**
 * Functional test for AZURE_PING discovery.
 *
 * @author Radoslav Husar
 * @version Jun 2015
 */
public class AZURE_PINGDiscoveryTest {

    public static final int CHANNEL_COUNT = 5;
    public static final String CLUSTER_NAME = "azure-cluster";

    @Test
    public void testDiscovery() throws Exception {

        List<JChannel> channels = create();

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

    private List<JChannel> create() throws Exception {
        List<JChannel> result = new LinkedList<>();
        for (int i = 0; i < CHANNEL_COUNT; i++) {
            JChannel channel = new JChannel(this.getClass().getResource("/org/jgroups/azure/protocols/tpc-azure.xml"));

            channel.connect(CLUSTER_NAME);
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
