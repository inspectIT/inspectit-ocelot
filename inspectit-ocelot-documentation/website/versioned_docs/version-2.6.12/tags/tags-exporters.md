---
id: version-2.6.12-tags-exporters
title: Tags Exporters
original_id: tags-exporters
---

:::warning
**This features is deprecated!** We recommend to propagate data via [baggage](instrumentation/data-propagation.md#baggage) instead, so the agent does not expose an API.
:::

Tags exporters represent special exporters of InspectIT, which allow to export internal tags to external applications 
like frontends running in browsers.
Currently, there is only one tags exporter:

| Exporter                        | Supports run-time updates | Push / Pull | Enabled by default |
|---------------------------------|---------------------------|-------------|--------------------|
| [HTTP Exporter](#http-exporter) | Yes                       | Push & Pull | No                 |

## HTTP Exporter

The HTTP exporter exports tags via a REST-API running on an HTTP-server. The server provides two endpoints.
One GET-endpoint to expose data to external applications and one PUT-endpoint to receive data from external applications.
The server is by default started on the port `9000` and data can then be accessed or written by 
calling 'http://localhost:9000/inspectit'

#### Production Environment

The Tags HTTP exporter does not provide any encryption of data and does not perform any authentication.
Thus, this server should not be exposed directly to the public in a production environment.
It is recommended to set up a proxy in front of this server, which handles encryption and authentication.

Furthermore, please make sure to enable port forwarding, if you use servlet-containers like tomcat.
You should also set _-Dinspectit.exporters.tags.http.host=0.0.0.0_ as parameter in the tomcat start configuration.

Additionally, make sure that your firewall is not blocking the HTTP-server address.

The server performs authorization with checking, whether the request origin is allowed to access the server. 
Additionally, every request has to provide a session ID to access their own session data.

#### Session Identification

Data tags will always be stored behind a provided session ID to ensure data correlation with its remote service.
The session ID will be read from a specific request-header. The 
_**inspectit.instrumentation.sessions.session-id-header**_ options allows to specify, which exact header 
should be used to read the session ID from. 

The default instrumentation of inspectIT will check the specified _session-id-header_ for a valid session ID. 
Thus, there is no additional configuration necessary to read session ID from HTTP-headers.

Behind every session ID, there is a data storage containing all data tags for this session, as long as they are 
enabled for [browser-propagation](instrumentation/data-propagation.md#browser-propagation) or 
[session-storage](instrumentation/data-propagation.md#session-storage).
A data storage will be created, if an HTTP request to the target application contains a session ID inside the
_session_id_header_. If a data storage already exists for the specified
session ID, no new data storage will be created.

You cannot create new data storages for example by pushing data into the HTTP-server by using the API. 
If a request to the REST-API contains a session ID, which does not exist in inspectIT, the API will always return 404.

For more detailed information, view the section [Session Storage](instrumentation/data-propagation.md#session-storage).

#### Runtime Updates

All properties of the HTTP-exporter can be updated during runtime. However, changing properties will result in a server
restart, which also deletes all data currently stored in the server.

The following properties are nested properties below the `inspectit.exporters.tags.http` property:

| Property             | Default      | Description                                                                                                 |
|----------------------|--------------|-------------------------------------------------------------------------------------------------------------|
| `.enabled`           | `DISABLED`   | If `ENABLED` or `IF_CONFIGURED`, the inspectIT Ocelot agent will try to start the exporter and HTTP server. |
| `.host`              | `127.0.0.1`  | The hostname or network address to which the HTTP server should bind.                                       |
| `.port`              | `9000`       | The port the HTTP server should use.                                                                        |
| `.path`              | `/inspectit` | The path on which the HTTP endpoints will be available.                                                     |
| `.thread-limit`      | `100`        | How many threads at most can process requests.                                                              |
| `.allowed-origins`   | `["*"]`      | A list of allowed origins, which are able to access the http-server.                                        | |

The data of the HTTP exporter is stored inside internal data storages. Data tags will only be written to the storage,
if they are enabled for [browser propagation](instrumentation/data-propagation.md#browser-propagation).

### Client Example

This example should demonstrate, how you can call the REST-API in your frontend application.

```javascript
// Send some requests to transfer the session-id to inspectIT
callBackend();

// Send GET-request
getTags();

// Send PUT-request
putTags();


function getTags() {
    const xhr = new XMLHttpRequest();
    const url = "http://localhost:9000/inspectit";

    xhr.open("GET", url);
    xhr.setRequestHeader("Session-Id", "my-very-awesome-session-id");

    xhr.onreadystatechange = () => {
        if (xhr.readyState === XMLHttpRequest.DONE) {
            if (xhr.status === 200) {
                let receivedData = xhr.responseText; // Read received data
                console.info(receivedData);
            } else {
                console.error("Error fetching data: ", xhr.status);
            }
        }
    };
    xhr.send();
}

function putTags() {
    const data = [
        {"service": "test-01"}, {"url": "www.example.com"}
    ]
    const xhr = new XMLHttpRequest();
    const url = "http://localhost:9000/inspectit";

    xhr.open("PUT", url);
    xhr.setRequestHeader("Session-Id", "my-very-awesome-session-id");
    
    xhr.onreadystatechange = () => {
        if (xhr.readyState === XMLHttpRequest.DONE) {
            if (xhr.status === 200) {
                console.log("Data fetched successfully!");
            } else {
                console.error("Error fetching data: ", xhr.status);
            }
        }
    };
    xhr.send(JSON.stringify(data));
}
```

### OpenAPI Documentation

Below you can see the OpenAPI documentation for the REST-API in YAML-format:

```yaml
openapi: 3.0.0
info:
  title: Tags Http Exporter
  description: |
    The API provides access to data tags, which are stored on the Tags HTTP-server of a InspectIT-java-agent. One data tag consists of a key-value-pair.
    Data Tags will be stored in the server, if they are enabled for browser-propagation in the InspectIT configuration server. 
    Using the API, those tags can be read, written and overwritten. However, every data tag will be stored behind a session-ID by InspectIT.

    In order to use a session-ID for storing data tags, a request has to been sent to the target application, which the InspectIT-java-agent is attached to.
    This request should contain the session-ID in it's headers. 
    All data tags created by this request, will be stored behind the provided session-ID as long as they are enabled for browser-propagation.

    To access specific data tags on the server via this API, requests will also need to contain the corresponding session-ID inside their header.
    Which header should be used to read the session-ID, can be configured in the InspectIT configuration server. By default, the header "Session-Id" will be used.
    Data tags are only cached for a specific amount of time, which is also defined in the InspectIT configuration server.
  contact:
    name: Novatec-Consulting GmbH
    url: https://inspectit.rocks/
    email: vhv-team@novatec-gmbh.de
  license:
    name: Apache 2.0
    url: http://www.apache.org/licenses/LICENSE-2.0
  version: 1.0.0
servers:
  - url: http://localhost:9000/inspectit
    description: default-URL of the API. However, the host, port and path can be configured in the InspectIT configuration-server
paths:
  /inspectit:
    summary: Single path of the API
    description: Single path of the API, which can be configured in the InspectIT configuration-server
    get:
      summary: Read currently stored data tags
      description: |
        Provides all currently stored data tags for the specified session in the session-ID. data tags will be returend as a set of map-entries.
        Data tags will only be stored in the tags-server, if they are enabled for browser-propagation.

        Note that all data tags are only cached for a specific amount of time, which can be configured in the InspectIT configuration server.
      parameters:
        - name: Session-Id
          in: header
          description: |
            Custom header with the ID of the current session. The session-ID-header-name can be configured in the InspectIT configuration-server.
            By default, "Session-Id" will be used as the session-ID-header-name.

            The length of the session-id is restricted to a minimum of 16 characters and a maximum of 512 characters.
          required: true
          schema:
            type: string
      responses:
        '200':
          description: Success - Response contains all current data tags
          content:
            application/json:
              schema:
                type: array
                items:
                  type: object
                  additionalProperties:
                    type: string
              example: '[{"key1": "value1"}, {"key2": "value2"}]'
        '400':
          description: Failure - Missing session-ID-header
        '403':
          description: Forbidden - Not allowed CORS headers
        '404':
          description: Failure - Session-ID not found in session-ID-header
    put:
      summary: Write or overwrite data tags
      description: |
        Overwrites data tags that are already stored in the Tags HTTP-server for the specified session-ID.
        Alternatively, write new data tags into the storage, to allow the InspectIT-java-agent to use tags, which are not available inside the JVM. 

        However, new data tags can only be written, if there already exists a data tag storage for the provided session-ID.
        It is not possible to create new data tag storages through this API, but only within the InspectIT-java-agent, by sending request to the target application.

        Note that these new data tags also have to be enabled for browser-propagation as well as down-propagation in the InspectIT configuration server.
      parameters:
        - name: Session-Id
          in: header
          description: |
            Custom header with the ID of the current session. The session-ID-header-name can be configured in the InspectIT configuration-server.
            By default, "Session-Id" will be used as the session-ID-header-name.

            The length of the session-id is restricted to a minimum of 16 characters and a maximum of 512 characters.
          required: true
          schema:
            type: string
      requestBody:
        description: data tags should be written or overwritten
        required: true
        content:
          application/json:
            schema:
              type: array
              items:
                type: object
                additionalProperties:
                  type: string
            example: '[{"key1": "value1"}, {"key2": "value2"}]'
      responses:
        '200':
          description: Success - Data tags have been written
        '400':
          description: Failure - Invalid request body or missing session-ID-header
        '403':
          description: Forbidden - Not allowed CORS headers
        '404':
          description: Failure - Session-ID not found in session-ID-header
    options:
      summary: Cross-Origin safety check
      description: |
        Allows to send pre-flight-requests to the API before actual requests. The usage is voluntary.
      parameters:
        - name: Origin
          in: header
          required: true
          schema:
            type: string
        - name: Access-Control-Request-Method
          in: header
          required: true
          schema:
            type: string
        - name: Access-Control-Request-Headers
          in: header
          required: true
          schema:
            type: string
      responses:
        '200':
          description: Success - Pre-flight response with CORS headers
          headers:
            Access-Control-Allow-Origin:
              schema:
                type: string
            Access-Control-Allow-Methods:
              schema:
                type: string
            Access-Control-Allow-Headers:
              schema:
                type: string
            Access-Control-Allow-Credentials:
              schema:
                type: boolean
        '403':
          description: Forbidden - Missing required headers
externalDocs:
  description: More information about the Exporter API
  url: https://inspectit.github.io/inspectit-ocelot/docs/tags/tags-exporters
```
