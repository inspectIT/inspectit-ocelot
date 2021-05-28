import React from 'react';

import AgentConfigurationDialog from './AgentConfigurationDialog';

export default {
  title: 'Components/Views/Status/Dialogs/AgentConfigurationDialog',
  component: AgentConfigurationDialog,
};

const Template = (args) => <AgentConfigurationDialog {...args} />;

export const ConfigurationDialog = Template.bind({});
ConfigurationDialog.args = {
  visible: true,
  configurationValue:
    "inspectit:\n  instrumentation:\n    scopes:\n      's_apacheclient_doExecute':\n        interfaces:\n          - name: 'org.apache.http.impl.client.CloseableHttpClient'\n        methods:\n          - name: 'doExecute'\n      \n      's_httpurlconnection_getOutputStream':\n        type:\n          name: 'java.net.HttpURLConnection'\n        methods:\n          - name: 'getOutputStream'\n            arguments: []\n\n      's_httpurlconnection_getInputStream':\n        superclass:\n          name: 'java.net.HttpURLConnection'\n        methods:\n          - name: 'getInputStream'\n            arguments: []\n\n      's_httpurlconnection_connect':\n        superclass:\n          name: 'java.net.HttpURLConnection'\n        methods:\n          - name: 'connect'\n            arguments: []\n\n      's_httpurlconnection_requestInitiators':\n        superclass:\n          name: 'java.net.HttpURLConnection'\n        methods:\n          - name: 'getHeaderField'\n          - name: 'getHeaderFieldDate'\n          - name: 'getHeaderFieldKey'\n          - name: 'getResponseCode'\n          - name: 'getResponseMessage'",
  error: false,
  loading: false,
  agentName: 'inspectit-agent',
};

export const LoadingAgentConfigurationDialog = Template.bind({});
LoadingAgentConfigurationDialog.args = {
  visible: true,
  configurationValue:
    "inspectit:\n  instrumentation:\n    scopes:\n      's_apacheclient_doExecute':\n        interfaces:\n          - name: 'org.apache.http.impl.client.CloseableHttpClient'\n        methods:\n          - name: 'doExecute'\n      \n      's_httpurlconnection_getOutputStream':\n        type:\n          name: 'java.net.HttpURLConnection'\n        methods:\n          - name: 'getOutputStream'\n            arguments: []\n\n      's_httpurlconnection_getInputStream':\n        superclass:\n          name: 'java.net.HttpURLConnection'\n        methods:\n          - name: 'getInputStream'\n            arguments: []\n\n      's_httpurlconnection_connect':\n        superclass:\n          name: 'java.net.HttpURLConnection'\n        methods:\n          - name: 'connect'\n            arguments: []\n\n      's_httpurlconnection_requestInitiators':\n        superclass:\n          name: 'java.net.HttpURLConnection'\n        methods:\n          - name: 'getHeaderField'\n          - name: 'getHeaderFieldDate'\n          - name: 'getHeaderFieldKey'\n          - name: 'getResponseCode'\n          - name: 'getResponseMessage'",
  error: false,
  loading: true,
  agentName: 'inspectit-agent',
};

export const FileConfigurationDialog = Template.bind({});
FileConfigurationDialog.args = {
  visible: true,
  configurationValue:
    "# {\"type\": \"Method-Configuration\"}\ninspectit:\n  instrumentation:\n    scopes:\n      's_apacheclient_doExecute':\n        interfaces:\n          - name: 'org.apache.http.impl.client.CloseableHttpClient'\n        methods:\n          - name: 'doExecute'\n      \n      's_httpurlconnection_getOutputStream':\n        type:\n          name: 'java.net.HttpURLConnection'\n        methods:\n          - name: 'getOutputStream'\n            arguments: []\n\n      's_httpurlconnection_getInputStream':\n        superclass:\n          name: 'java.net.HttpURLConnection'\n        methods:\n          - name: 'getInputStream'\n            arguments: []\n\n      's_httpurlconnection_connect':\n        superclass:\n          name: 'java.net.HttpURLConnection'\n        methods:\n          - name: 'connect'\n            arguments: []\n\n      's_httpurlconnection_requestInitiators':\n        superclass:\n          name: 'java.net.HttpURLConnection'\n        methods:\n          - name: 'getHeaderField'\n          - name: 'getHeaderFieldDate'\n          - name: 'getHeaderFieldKey'\n          - name: 'getResponseCode'\n          - name: 'getResponseMessage'",
  loading: false,
  fileName: '/method-configuration.yml',
};

export const LoadingFileConfigurationDialog = Template.bind({});
LoadingFileConfigurationDialog.args = {
  visible: true,
  configurationValue:
    "# {\"type\": \"Method-Configuration\"}\ninspectit:\n  instrumentation:\n    scopes:\n      's_apacheclient_doExecute':\n        interfaces:\n          - name: 'org.apache.http.impl.client.CloseableHttpClient'\n        methods:\n          - name: 'doExecute'\n      \n      's_httpurlconnection_getOutputStream':\n        type:\n          name: 'java.net.HttpURLConnection'\n        methods:\n          - name: 'getOutputStream'\n            arguments: []\n\n      's_httpurlconnection_getInputStream':\n        superclass:\n          name: 'java.net.HttpURLConnection'\n        methods:\n          - name: 'getInputStream'\n            arguments: []\n\n      's_httpurlconnection_connect':\n        superclass:\n          name: 'java.net.HttpURLConnection'\n        methods:\n          - name: 'connect'\n            arguments: []\n\n      's_httpurlconnection_requestInitiators':\n        superclass:\n          name: 'java.net.HttpURLConnection'\n        methods:\n          - name: 'getHeaderField'\n          - name: 'getHeaderFieldDate'\n          - name: 'getHeaderFieldKey'\n          - name: 'getResponseCode'\n          - name: 'getResponseMessage'",
  loading: true,
  fileName: '/method-configuration.yml',
};
