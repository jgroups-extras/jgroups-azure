# JGroups Azure

[![Build Status](https://travis-ci.org/rhusar/jgroups-azure.svg?branch=master)](https://travis-ci.org/rhusar/jgroups-azure)

Implementation of Azure ping protocol using Azure Storage blobs. Makes use of official Microsoft
Azure Java SDK.


## Usage

via Maven

```xml
<dependency>
    <groupId>org.jgroups</groupId>
    <artifactId>jgroups-azure</artifactId>
    <version>1.0.0.Alpha1-SNAPSHOT</version>
</dependency>
```

## Configuration

### WildFly

First add the required modules then replace the existing discovery protocol (e.g. MPING) with

```xml
	<org.jgroups.azure.protocols.AZURE_PING module="org.jgroups.azure">
		<property name="storage_account_name">${azure.account_name}</property>
		<property name="storage_access_key">${azure.access_key}</property>
		<property name="container">${azure.container:ping}</property>
	</org.jgroups.azure.protocols.AZURE_PING>
```

### Directly in JGroups

Add the protocol to the stack

```xml
	<org.jgroups.azure.protocols.AZURE_PING
		storage_account_name="${azure.account_name}"
		storage_access_key="${azure.access_key}"
		container="${azure.container:ping}"
	/>
```

## Building

Use Maven to build

    $ mvn install


## Testing

The tests expect valid credentials for Azure, you can supply using properties:

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

