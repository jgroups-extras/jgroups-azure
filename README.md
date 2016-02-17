# JGroups Azure

[![Build Status](https://travis-ci.org/rhusar/jgroups-azure.svg?branch=master)](https://travis-ci.org/rhusar/jgroups-azure)

Implementation of Azure ping protocol using Azure Storage blobs. Makes use of official Microsoft
Azure Java SDK.

## Configuration

### WildFly

First copy the required modules (from `dist/target/wildfly-jgroups-azure-<version>/modules`) to the WildFly installation,
then replace the existing discovery protocol (PING, MPING, etc.) with the following:

```xml
<protocol type="azure.AZURE_PING" module="org.jgroups.azure">
    <property name="storage_account_name">
         ${jboss.jgroups.azure_ping.storage_account_name}
    </property>
    <property name="storage_access_key">
         ${jboss.jgroups.azure_ping.storage_access_key}
    </property>
    <property name="container">
         ${jboss.jgroups.azure_ping.container}
    </property>
 </protocol>
```

Following WildFly 10.1/11.0 the modules will be bundled in the distribution, use directly have profile located at
`wildfly-<version>/docs/examples/configs/standalone-azure-ha.xml` or replace the existing discovery protocol with
the following configuration (no need to specify module name):

```xml
<protocol type="azure.AZURE_PING">
     <property name="storage_account_name">
         ${jboss.jgroups.azure_ping.storage_account_name}
     </property>
     <property name="storage_access_key">
         ${jboss.jgroups.azure_ping.storage_access_key}
     </property>
     <property name="container">
         ${jboss.jgroups.azure_ping.container}
     </property>
</protocol>
```

### Directly in JGroups

You can bring in all dependencies via Maven:

```xml
<dependency>
    <groupId>org.jgroups</groupId>
    <artifactId>jgroups-azure</artifactId>
    <version>${version.org.jgroups.azure}</version>
</dependency>
```

Then add or replace an existing discovery protocol in the stack:

```xml
<azure.AZURE_PING
	storage_account_name="${jboss.jgroups.azure_ping.storage_account_name}"
	storage_access_key="${jboss.jgroups.azure_ping.storage_access_key}"
	container="${jboss.jgroups.azure_ping.container:ping}"
/>
```

## Building

Use Maven to build:

    $ mvn install -DskipTests

_Note that running the tests requires access to Azure._


## Testing

The tests expect valid credentials for Azure which you can supply using properties:

    $ mvn test -Dazure.account_name="A" -Dazure.access_key="B" -Djava.net.preferIPv4Stack=true



## License

    Copyright 2015 Red Hat Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

