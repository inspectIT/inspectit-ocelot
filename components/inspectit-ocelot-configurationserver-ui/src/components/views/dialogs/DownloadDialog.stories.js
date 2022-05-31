import React from 'react';

import DownloadDialog from './DownloadDialog';

export default {
  title: 'Components/Views/Status/Dialogs/DownloadDialog',
  component: DownloadDialog,
};

const Template = (args) => <DownloadDialog {...args} />;

export const AgentConfigurationDialog = Template.bind({});
AgentConfigurationDialog.args = {
  visible: true,
  contentValue:
    "inspectit:\n  instrumentation:\n    scopes:\n      's_apacheclient_doExecute':\n        interfaces:\n          - name: 'org.apache.http.impl.client.CloseableHttpClient'\n        methods:\n          - name: 'doExecute'\n      \n      's_httpurlconnection_getOutputStream':\n        type:\n          name: 'java.net.HttpURLConnection'\n        methods:\n          - name: 'getOutputStream'\n            arguments: []\n\n      's_httpurlconnection_getInputStream':\n        superclass:\n          name: 'java.net.HttpURLConnection'\n        methods:\n          - name: 'getInputStream'\n            arguments: []\n\n      's_httpurlconnection_connect':\n        superclass:\n          name: 'java.net.HttpURLConnection'\n        methods:\n          - name: 'connect'\n            arguments: []\n\n      's_httpurlconnection_requestInitiators':\n        superclass:\n          name: 'java.net.HttpURLConnection'\n        methods:\n          - name: 'getHeaderField'\n          - name: 'getHeaderFieldDate'\n          - name: 'getHeaderFieldKey'\n          - name: 'getResponseCode'\n          - name: 'getResponseMessage'",
  error: false,
  loading: false,
  contentType: 'config',
  contextName: 'inspectit-agent',
};

export const LoadingAgentConfigurationDialog = Template.bind({});
LoadingAgentConfigurationDialog.args = {
  visible: true,
  contentValue:
    "inspectit:\n  instrumentation:\n    scopes:\n      's_apacheclient_doExecute':\n        interfaces:\n          - name: 'org.apache.http.impl.client.CloseableHttpClient'\n        methods:\n          - name: 'doExecute'\n      \n      's_httpurlconnection_getOutputStream':\n        type:\n          name: 'java.net.HttpURLConnection'\n        methods:\n          - name: 'getOutputStream'\n            arguments: []\n\n      's_httpurlconnection_getInputStream':\n        superclass:\n          name: 'java.net.HttpURLConnection'\n        methods:\n          - name: 'getInputStream'\n            arguments: []\n\n      's_httpurlconnection_connect':\n        superclass:\n          name: 'java.net.HttpURLConnection'\n        methods:\n          - name: 'connect'\n            arguments: []\n\n      's_httpurlconnection_requestInitiators':\n        superclass:\n          name: 'java.net.HttpURLConnection'\n        methods:\n          - name: 'getHeaderField'\n          - name: 'getHeaderFieldDate'\n          - name: 'getHeaderFieldKey'\n          - name: 'getResponseCode'\n          - name: 'getResponseMessage'",
  error: false,
  loading: true,
  contentType: 'config',
  contextName: 'inspectit-agent',
};

export const FileConfigurationDialog = Template.bind({});
FileConfigurationDialog.args = {
  visible: true,
  contentValue:
    "# {\"type\": \"Method-Configuration\"}\ninspectit:\n  instrumentation:\n    scopes:\n      's_apacheclient_doExecute':\n        interfaces:\n          - name: 'org.apache.http.impl.client.CloseableHttpClient'\n        methods:\n          - name: 'doExecute'\n      \n      's_httpurlconnection_getOutputStream':\n        type:\n          name: 'java.net.HttpURLConnection'\n        methods:\n          - name: 'getOutputStream'\n            arguments: []\n\n      's_httpurlconnection_getInputStream':\n        superclass:\n          name: 'java.net.HttpURLConnection'\n        methods:\n          - name: 'getInputStream'\n            arguments: []\n\n      's_httpurlconnection_connect':\n        superclass:\n          name: 'java.net.HttpURLConnection'\n        methods:\n          - name: 'connect'\n            arguments: []\n\n      's_httpurlconnection_requestInitiators':\n        superclass:\n          name: 'java.net.HttpURLConnection'\n        methods:\n          - name: 'getHeaderField'\n          - name: 'getHeaderFieldDate'\n          - name: 'getHeaderFieldKey'\n          - name: 'getResponseCode'\n          - name: 'getResponseMessage'",
  loading: false,
  contentType: 'config',
  contextName: 'inspectit-agent',
};

export const LoadingFileConfigurationDialog = Template.bind({});
LoadingFileConfigurationDialog.args = {
  visible: true,
  contentValue:
    "# {\"type\": \"Method-Configuration\"}\ninspectit:\n  instrumentation:\n    scopes:\n      's_apacheclient_doExecute':\n        interfaces:\n          - name: 'org.apache.http.impl.client.CloseableHttpClient'\n        methods:\n          - name: 'doExecute'\n      \n      's_httpurlconnection_getOutputStream':\n        type:\n          name: 'java.net.HttpURLConnection'\n        methods:\n          - name: 'getOutputStream'\n            arguments: []\n\n      's_httpurlconnection_getInputStream':\n        superclass:\n          name: 'java.net.HttpURLConnection'\n        methods:\n          - name: 'getInputStream'\n            arguments: []\n\n      's_httpurlconnection_connect':\n        superclass:\n          name: 'java.net.HttpURLConnection'\n        methods:\n          - name: 'connect'\n            arguments: []\n\n      's_httpurlconnection_requestInitiators':\n        superclass:\n          name: 'java.net.HttpURLConnection'\n        methods:\n          - name: 'getHeaderField'\n          - name: 'getHeaderFieldDate'\n          - name: 'getHeaderFieldKey'\n          - name: 'getResponseCode'\n          - name: 'getResponseMessage'",
  loading: true,
  contentType: 'config',
  contextName: 'inspectit-agent',
};

export const AgentLogDialog = Template.bind({});
AgentLogDialog.args = {
  visible: true,
  contentValue:
    '2022-06-02 08:47:30,769 INFO  34095  --- [inspectIT] [pectit-thread-1] ### LOG-INVALIDATING EVENT ###           : Property sources reload! Some previous log messages may now be outdated.\n' +
    "2022-06-02 08:47:31,912 WARN  35238  --- [inspectIT] [pectit-thread-0] r.i.o.c.exporter.JaegerExporterService   : The property 'protocol' was not set. Based on the set property 'url' we assume the protocol 'http/thrift'. This fallback will be removed in future releases. Please make sure to use the property 'protocol' in future.\n" +
    "2022-06-02 08:47:31,912 WARN  35238  --- [inspectIT] [pectit-thread-0] r.i.o.c.exporter.JaegerExporterService   : You are using the deprecated property 'url'. This property will be invalid in future releases of InspectIT Ocelot, please use 'endpoint' instead.\n" +
    '2022-06-02 08:47:31,922 INFO  35248  --- [inspectIT] [pectit-thread-1] ### LOG-INVALIDATING EVENT ###           : Instrumentation configuration changed! Some previous log messages may now be outdated.\n' +
    '2022-06-02 12:27:02,943 INFO  13206269 --- [inspectIT] [pectit-thread-0] ### LOG-INVALIDATING EVENT ###           : Property sources reload! Some previous log messages may now be outdated.\n' +
    "2022-06-02 12:27:03,473 WARN  13206799 --- [inspectIT] [pectit-thread-0] r.i.o.c.exporter.JaegerExporterService   : The property 'protocol' was not set. Based on the set property 'url' we assume the protocol 'http/thrift'. This fallback will be removed in future releases. Please make sure to use the property 'protocol' in future.\n" +
    "2022-06-02 12:27:03,474 WARN  13206800 --- [inspectIT] [pectit-thread-0] r.i.o.c.exporter.JaegerExporterService   : You are using the deprecated property 'url'. This property will be invalid in future releases of InspectIT Ocelot, please use 'endpoint' instead.\n" +
    '2022-06-02 12:27:03,480 INFO  13206806 --- [inspectIT] [pectit-thread-0] ### LOG-INVALIDATING EVENT ###           : Instrumentation configuration changed! Some previous log messages may now be outdated.',
  error: false,
  loading: false,
  contentType: 'log',
  contextName: 'inspectit-agent',
};

export const LoadingAgentLogDialog = Template.bind({});
LoadingAgentLogDialog.args = {
  visible: true,
  contentValue: '',
  error: false,
  loading: true,
  contentType: 'log',
  contextName: 'inspectit-agent',
};
