# JGroups Azure

*Support branch for EAP 6.4.15 (or higher) and for JGroups 3.2.16 (or higher).*

Implementation of Azure ping protocol using Azure Storage blobs. Makes use of official Microsoft
Azure Java SDK.

## Configuration

### JBoss EAP 6.4

#### Upgrading an existing profile via CLI

To upgrade an existing profile to create new stack with AZURE_PING protocol via CLI, start `./bin/jboss-cli.sh`
(or corresponding `bat` script on Windows) and run the following batch followed by a reload:

```
batch
/subsystem=jgroups/stack=azure:add
/subsystem=jgroups/stack=azure/transport=TRANSPORT:add(type=TCP,socket-binding=jgroups-tcp)
/subsystem=jgroups/stack=azure/:add-protocol(type=azure.AZURE_PING)
/subsystem=jgroups/stack=azure/protocol=azure.AZURE_PING/property=container:add(value=${jboss.jgroups.azure_ping.container})
/subsystem=jgroups/stack=azure/protocol=azure.AZURE_PING/property=storage_account_name:add(value=${jboss.jgroups.azure_ping.storage_account_name})
/subsystem=jgroups/stack=azure/protocol=azure.AZURE_PING/property=storage_access_key:add(value=${jboss.jgroups.azure_ping.storage_access_key})
/subsystem=jgroups/stack=azure/:add-protocol(type=MERGE2
/subsystem=jgroups/stack=azure/:add-protocol(type=FD_SOCK,socket-binding=jgroups-tcp-fd)
/subsystem=jgroups/stack=azure/:add-protocol(type=FD
/subsystem=jgroups/stack=azure/:add-protocol(type=VERIFY_SUSPECT
/subsystem=jgroups/stack=azure/:add-protocol(type=pbcast.NAKACK
/subsystem=jgroups/stack=azure/:add-protocol(type=UNICAST2
/subsystem=jgroups/stack=azure/:add-protocol(type=pbcast.STABLE
/subsystem=jgroups/stack=azure/:add-protocol(type=pbcast.GMS
/subsystem=jgroups/stack=azure/:add-protocol(type=UFC
/subsystem=jgroups/stack=azure/:add-protocol(type=MFC
/subsystem=jgroups/stack=azure/:add-protocol(type=FRAG2
/subsystem=jgroups/stack=azure/:add-protocol(type=RSVP
/subsystem=jgroups:write-attribute(name=default-stack, value=azure)
run-batch
/:reload
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



## License

    Copyright 2017 Red Hat Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

