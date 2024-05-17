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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.List;

import com.azure.identity.DefaultAzureCredentialBuilder;

import com.azure.storage.blob.*;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.ListBlobsOptions;
import com.azure.storage.common.StorageSharedKeyCredential;
import org.jgroups.Address;
import org.jgroups.annotations.Property;
import org.jgroups.conf.ClassConfigurator;
import org.jgroups.logging.Log;
import org.jgroups.logging.LogFactory;
import org.jgroups.protocols.FILE_PING;
import org.jgroups.protocols.PingData;
import org.jgroups.util.Responses;

/**
 * Implementation of PING protocols for AZURE using Storage Blobs. See /DESIGN.md for design.
 *
 * @author Radoslav Husar
 */
public class AZURE_PING extends FILE_PING {

    private static final Log log = LogFactory.getLog(AZURE_PING.class);

    @Property(description = "The name of the storage account.")
    protected String storage_account_name;

    @Property(description = "The secret account access key, can be left blank to try to obtain credentials from the environment.", exposeAsManagedAttribute = false)
    protected String storage_access_key;

    @Property(description = "Container to store ping information in. Must be valid DNS name.")
    protected String container;

    @Property(description = "Whether or not to use HTTPS to connect to Azure.")
    protected boolean use_https = true;

    @Property(description = "The endpointSuffix to use.")
    protected String endpoint_suffix;

    public static final int STREAM_BUFFER_SIZE = 4096;

    private BlobContainerClient containerClient;

    private static final Duration TIMEOUT = Duration.ofSeconds(1);

    static {
        ClassConfigurator.addProtocol((short) 530, AZURE_PING.class);
    }

    @Override
    public void init() throws Exception {
        super.init();

        // Validate configuration
        // Can throw IAEs
        this.validateConfiguration();

        final BlobServiceClientBuilder blobClientBuilder = new BlobServiceClientBuilder();
        if (storage_access_key != null) {
            blobClientBuilder.credential(new StorageSharedKeyCredential(storage_account_name, storage_access_key));
        } else {
            blobClientBuilder.credential(new DefaultAzureCredentialBuilder().build());
        }

        blobClientBuilder.endpoint(new BlobUrlParts()
                .setAccountName(storage_account_name)
                .setScheme(use_https ? "https" : "http")
                .setHost(endpoint_suffix) // endpoint suffix = host base component
                .toUrl().toString());


        try {
            BlobServiceClient blobClient = blobClientBuilder.buildClient();
            containerClient = blobClient.getBlobContainerClient(container);

            boolean created = containerClient.createIfNotExists();
            if (created) {
                log.info("Created container named '%s'.", container);
            } else {
                log.debug("Using existing container named '%s'.", container);
            }

        } catch (Exception ex) {
            log.error("Error creating a storage client! Check your configuration.");
            throw ex;
        }
    }

    public void validateConfiguration() throws IllegalArgumentException {
        // Validate that container name is configured and must be all lowercase
        if (container == null || !container.toLowerCase().equals(container) || container.contains("--")
                || container.startsWith("-") || container.length() < 3 || container.length() > 63) {
            throw new IllegalArgumentException("Container name must be configured and must meet Azure requirements (must be a valid DNS name).");
        }
        // Account name and must be configured
        if (storage_account_name == null) {
            throw new IllegalArgumentException("Account name must be configured.");
        }
        // Let's inform users here that https would be preferred
        if (!use_https) {
            log.info("Configuration is using HTTP, consider switching to HTTPS instead.");
        }

    }

    @Override
    protected void createRootDir() {
        // Do not remove this!
        // There is no root directory to create, overriding here with noop.
    }

    @Override
    protected void readAll(final List<Address> members, final String clustername, final Responses responses) {
        if (clustername == null) {
            return;
        }

        String prefix = sanitize(clustername);

        Iterable<BlobItem> blobItems = containerClient.listBlobs(new ListBlobsOptions().setPrefix(prefix), TIMEOUT);
        for (BlobItem blobItem : blobItems) {
            try {
                // If the item is a blob and not a virtual directory.
                if (!blobItem.isPrefix()) {
                    BlobClient blobClient = containerClient.getBlobClient(blobItem.getName());
                    ByteArrayOutputStream os = new ByteArrayOutputStream(STREAM_BUFFER_SIZE);
                    blobClient.downloadStream(os);
                    byte[] pingBytes = os.toByteArray();
                    parsePingData(pingBytes, members, responses);
                }
            } catch (Exception t) {
                log.error("Error fetching ping data.");
            }
        }
    }

    protected void parsePingData(final byte[] pingBytes, final List<Address> members, final Responses responses) {
        if (pingBytes == null || pingBytes.length <= 0) {
            return;
        }
        List<PingData> list;
        try {
            list = read(new ByteArrayInputStream(pingBytes));
            if (list != null) {
                // This is a common piece of logic for all PING protocols copied from org/jgroups/protocols/FILE_PING.java:245
                // Maybe could be extracted for all PING impls to share this logic?
                for (PingData data : list) {
                    if (members == null || members.contains(data.getAddress())) {
                        responses.addResponse(data, data.isCoord());
                    }
                    if (local_addr != null && !local_addr.equals(data.getAddress())) {
                        addDiscoveryResponseToCaches(data.getAddress(), data.getLogicalName(), data.getPhysicalAddr());
                    }
                }
                // end copied block
            }
        } catch (Exception e) {
            log.error("Error unmarshalling ping data.", e);
        }
    }

    @Override
    protected void write(final List<PingData> list, final String clustername) {
        if (list == null || clustername == null) {
            return;
        }

        String filename = addressToFilename(clustername, local_addr);
        ByteArrayOutputStream out = new ByteArrayOutputStream(STREAM_BUFFER_SIZE);

        try {
            write(list, out);
            byte[] data = out.toByteArray();

            // Upload the file
            BlobClient blobClient = containerClient.getBlobClient(filename);
            blobClient.upload(new ByteArrayInputStream(data), data.length);

        } catch (Exception ex) {
            log.error("Error marshalling and uploading ping data.", ex);
        }

    }

    @Override
    protected void remove(final String clustername, final Address addr) {
        if (clustername == null || addr == null) {
            return;
        }

        String filename = addressToFilename(clustername, addr);

        try {
            BlobClient blobClient = containerClient.getBlobClient(filename);
            boolean deleted = blobClient.deleteIfExists();

            if (deleted) {
                log.debug("Tried to delete file '%s' but it was already deleted.", filename);
            } else {
                log.trace("Deleted file '%s'.", filename);
            }

        } catch (Exception ex) {
            log.error("Error deleting files.", ex);
        }
    }

    @Override
    protected void removeAll(String clustername) {
        if (clustername == null) {
            return;
        }

        clustername = sanitize(clustername);

        Iterable<BlobItem> blobItems = containerClient.listBlobs(
                new ListBlobsOptions().setPrefix(clustername), TIMEOUT);
        for (BlobItem blobItem : blobItems) {
            try {
                // If the item is a blob and not a virtual directory.
                if (!blobItem.isPrefix()) {
                    BlobClient blobClient = containerClient.getBlobClient(blobItem.getName());
                    boolean deleted = blobClient.deleteIfExists();
                    if (deleted) {
                        log.trace("Deleted file '%s'.", blobItem.getName());
                    } else {
                        log.debug("Tried to delete file '%s' but it was already deleted.", blobItem.getName());
                    }
                }
            } catch (Exception e) {
                log.error("Error deleting ping data for cluster '" + clustername + "'.", e);
            }
        }
    }

    /**
     * Converts cluster name and address into a filename.
     */
    protected static String addressToFilename(final String clustername, final Address address) {
        return sanitize(clustername) + "-" + addressToFilename(address);
    }

    /**
     * Sanitizes names replacing backslashes and forward slashes with a dash.
     */
    protected static String sanitize(final String name) {
        return name.replace('/', '-').replace('\\', '-');
    }


}
