# JGroups Azure

[![Build Status](https://travis-ci.org/jgroups-extras/jgroups-azure.svg?branch=master)](https://travis-ci.org/jgroups-extras/jgroups-azure)
[![Dependabot Status](https://api.dependabot.com/badges/status?host=github&repo=jgroups-extras/jgroups-azure)](https://dependabot.com)

Implementation of Azure ping protocol using Azure Storage blobs. Makes use of official Microsoft
Azure Java SDK.

## Configuration

### WildFly 10.1 / JBoss EAP 7.0

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
(or corresponding `bat` script on Windows) and run the following batch followed by a reload:

```
batch
/subsystem=jgroups/channel=ee:write-attribute(name=stack,value=tcp)
/subsystem=jgroups/stack=tcp/protocol=MPING:remove
/subsystem=jgroups/stack=tcp/protocol=azure.AZURE_PING:add(add-index=0,properties=[storage_account_name=${jboss.jgroups.azure_ping.storage_account_name:},storage_access_key=${jboss.jgroups.azure_ping.storage_access_key:},container=${jboss.jgroups.azure_ping.container:}])
run-batch
/:reload
```

_Note that due to issue [WFLY-6782](https://issues.jboss.org/browse/WFLY-6782) adding via CLI may fail on older versions._

### WildFly 10.0 and older

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

    $ mvn install -DskipTests

_Note that running the tests requires access to Azure._


## Testing

The tests expect valid credentials for Azure which you can supply using properties:

    $ mvn test -Dazure.account_name="A" -Dazure.access_key="B" -Djava.net.preferIPv4Stack=true



## Support Matrix

Branch | JGroups version | Azure Storage version | Java version
------ | --------------- | --------------------- | ------------
0.9    | 3.2.16.Final    | 5.0.0                 | 6
1.0    | 3.6.7.Final     | 4.0.0                 | 7
1.1    | 3.6.13.Final    | 5.0.0                 | 8
master | 4.x             | 6.1.0                 | 8, 9


## License

    Copyright 2020 Red Hat Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

