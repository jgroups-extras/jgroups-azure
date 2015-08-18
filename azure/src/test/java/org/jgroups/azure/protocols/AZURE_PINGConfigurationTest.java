package org.jgroups.azure.protocols;

import org.junit.Test;

/**
 * Unit test for AZURE_PING protocols configuration.
 *
 * @author Radoslav Husar
 * @version Jun 2015
 */
public class AZURE_PINGConfigurationTest {

    private AZURE_PING azure;

    public AZURE_PINGConfigurationTest() {
        azure = new AZURE_PING();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidationMissingConfiguration() {
        azure.validateConfiguration();
    }

    @Test
    public void testValidationAllConfigured() {
        azure.storage_account_name = "myaccount";
        azure.storage_access_key = "1wsRK265DNm8v7LxT6txU2qZ_3DsBnbv";
        azure.container = "mycontainer";
        azure.validateConfiguration();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidationWrongContainerName() {
        azure.storage_account_name = "myaccount";
        azure.storage_access_key = "1wsRK265DNm8v7LxT6txU2qZ_3DsBnbv";
        azure.container = "MY_CONTAINER";
        azure.validateConfiguration();
    }

}
