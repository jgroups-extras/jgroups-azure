/*
 * Copyright 2017 Red Hat Inc., and individual contributors
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
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.LinkedList;
import java.util.List;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageCredentials;
import com.microsoft.azure.storage.StorageCredentialsAccountAndKey;
import com.microsoft.azure.storage.blob.CloudBlob;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.blob.ListBlobItem;
import org.jgroups.Address;
import org.jgroups.annotations.Property;
import org.jgroups.conf.ClassConfigurator;
import org.jgroups.logging.Log;
import org.jgroups.logging.LogFactory;
import org.jgroups.protocols.FILE_PING;
import org.jgroups.protocols.PingData;

/**
 * Implementation of PING protocols for AZURE using Storage Blobs. See /DESIGN.md for design.
 *
 * @author Radoslav Husar
 */
public class AZURE_PING extends FILE_PING {

    private static final Log log = LogFactory.getLog(AZURE_PING.class);

    @Property(description = "The name of the storage account.")
    protected String storage_account_name;

    @Property(description = "The secret account access key.", exposeAsManagedAttribute = false)
    protected String storage_access_key;

    @Property(description = "Container to store ping information in. Must be valid DNS name.")
    protected String container;

    @Property(description = "Whether or not to use HTTPS to connect to Azure.")
    protected boolean use_https = true;

    public static final int STREAM_BUFFER_SIZE = 4096;

    private CloudBlobContainer containerReference;

    static {
        ClassConfigurator.addProtocol((short) 530, AZURE_PING.class);
    }

    @Override
    public void init() throws Exception {
        super.init();

        // Validate configuration
        // Can throw IAEs
        this.validateConfiguration();

        try {
            StorageCredentials credentials = new StorageCredentialsAccountAndKey(storage_account_name, storage_access_key);
            CloudStorageAccount storageAccount = new CloudStorageAccount(credentials, use_https);
            CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
            containerReference = blobClient.getContainerReference(container);
            boolean created = containerReference.createIfNotExists();

            if (created) {
                log.info("Created container named '" + container + "'.");
            } else {
                log.debug("Using existing container named '" + container + "'.");
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
        // Account name and access key must be both configured for write access
        if (storage_account_name == null || storage_access_key == null) {
            throw new IllegalArgumentException("Account name and key must be configured.");
        }
        // Lets inform users here that https would be preferred
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
    protected List<PingData> readAll(String clustername) {
        if (clustername == null) {
            return null;
        }
        List<PingData> pingData = new LinkedList<PingData>();

        String prefix = sanitize(clustername);

        Iterable<ListBlobItem> listBlobItems = containerReference.listBlobs(prefix);
        for (ListBlobItem blobItem : listBlobItems) {
            try {
                // If the item is a blob and not a virtual directory.
                // n.b. what an ugly API this is
                if (blobItem instanceof CloudBlob) {
                    CloudBlob blob = (CloudBlob) blobItem;
                    ByteArrayOutputStream os = new ByteArrayOutputStream(STREAM_BUFFER_SIZE);
                    blob.download(os);
                    byte[] pingBytes = os.toByteArray();
                    pingData.add(parsePingData(pingBytes));
                }
            } catch (Exception t) {
                log.error("Error fetching ping data.");
            }
        }

        return pingData;
    }

    private static PingData parsePingData(final byte[] pingBytes) throws Exception {
        if (pingBytes == null || pingBytes.length <= 0) {
            return null;
        }

        ByteArrayInputStream bis = new ByteArrayInputStream(pingBytes);
        DataInput di = new DataInputStream(bis);

        PingData tmp = new PingData();
        tmp.readFrom(di);
        return tmp;
    }

    @Override
    protected synchronized void writeToFile(PingData data, String clustername) {
        if (data == null || clustername == null) {
            return;
        }

        String filename = addressToFilename(clustername, local_addr);
        ByteArrayOutputStream out = new ByteArrayOutputStream(STREAM_BUFFER_SIZE);
        DataOutputStream dataOut = new DataOutputStream(out);

        try {
            data.writeTo(dataOut);
            byte[] uploadData = out.toByteArray();

            // Upload the file
            CloudBlockBlob blob = containerReference.getBlockBlobReference(filename);
            blob.upload(new ByteArrayInputStream(uploadData), uploadData.length);

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
            CloudBlockBlob blob = containerReference.getBlockBlobReference(filename);
            boolean deleted = blob.deleteIfExists();

            if (deleted) {
                log.debug("Tried to delete file '" + filename + "' but it was already deleted.");
            } else {
                log.trace("Deleted file '" + filename + "'.");
            }

        } catch (Exception ex) {
            log.error("Error deleting files.", ex);
        }
    }

    /**
     * Converts cluster name and address into a filename.
     */
    protected static String addressToFilename(final String clustername, final Address address) {
        return sanitize(clustername) + "-" + addressAsString(address);
    }

    /**
     * Sanitizes names replacing backslashes and forward slashes with a dash.
     */
    protected static String sanitize(final String name) {
        return name.replace('/', '-').replace('\\', '-');
    }


}
