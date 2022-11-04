# JGroups Azure

[![CI](https://github.com/jgroups-extras/jgroups-azure/workflows/CI/badge.svg)](https://github.com/jgroups-extras/jgroups-azure/actions)

Implementation of Azure ping protocol using Azure Storage blobs for [JGroups](http://www.jgroups.org/) project using official Microsoft
Azure Java SDK.

## Configuration

### Properties

Property name          | Description                                                     | Required / Default value
---------------------- | --------------------------------------------------------------- | ------------------------
`storage_account_name` | The name of the storage account.                                | _Required._
`storage_access_key`   | The secret account access key.                                  | _Required._
`container`            | Container to store ping information in. Must be valid DNS name. | _Required._
`use_https`            | Whether or not to use HTTPS to connect to Azure.                | `true`
`endpoint_suffix`      | The endpointSuffix to use.                                      |

### WildFly 10.1 or later / JBoss EAP 7.0 or later

#### Using a preconfigured profile

Since WildFly 10.1 the required modules are bundled with the distribution. Users can directly use server profile located at
`wildfly-<version>/docs/examples/configs/standalone-azure-ha.xml` or replace the discovery protocol in their existing 
server profiles with the following configuration (no need to specify module name):

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

#### Upgrading an existing profile via CLI

To upgrade an existing profile to use TCP stack by default and AZURE_PING protocol via CLI, start `./bin/jboss-cli.sh`
(or corresponding script on Windows) and run the following batch followed by a reload:

```
batch
/subsystem=jgroups/channel=ee:write-attribute(name=stack, value=tcp)
/subsystem=jgroups/stack=tcp/protocol=MPING:remove
/subsystem=jgroups/stack=tcp/protocol=azure.AZURE_PING:add(add-index=0, properties=[storage_account_name=${jboss.jgroups.azure_ping.storage_account_name:}, storage_access_key=${jboss.jgroups.azure_ping.storage_access_key:}, container=${jboss.jgroups.azure_ping.container:}])
run-batch
reload
```

### Directly in JGroups

You can bring in all dependencies via Maven:

```xml
<dependency>
    <groupId>org.jgroups.azure</groupId>
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

    mvn clean install

Or use Maven wrapper for convenience:

    ./mvnw clean install

## Testing

To run all tests, credentials for Azure are expected. These can be supplied using test properties:

    export account_name=foo
    export access_key=bar
    mvn test -Dazure.account_name="${account_name}" -Dazure.access_key="${access_key}" -Djava.net.preferIPv4Stack=true

If valid credentials are not provided, tests requiring them are skipped.

## Support Matrix

Version (branch) | JGroups version | Azure Storage version | Java version
---------------- | --------------- | --------------------- | ------------
0.9              | 3.2.16.Final    | 5.0.0                 | 6
1.0              | 3.6.7.Final     | 4.0.0                 | 7
1.1              | 3.6.13.Final    | 5.0.0                 | 8
1.2              | 4.0.x           | 6.1.0                 | 8
1.3              | 4.2.x           | 8.6.6                 | 8
2.0 (main)       | 5.1.x           | 8.6.6                 | 11          


## License

    Copyright 2022 Red Hat Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

