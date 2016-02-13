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
