import React from 'react';

import MethodConfigurationEditor from './MethodConfigurationEditor';

export default {
  title: 'Components/Editor/MethodConfigurationEditor/MethodConfigurationEditor',
  component: MethodConfigurationEditor,
};

const Template = (args) => <MethodConfigurationEditor {...args} />;

export const Default = Template.bind({});
Default.args = {
  yamlConfiguration:
    "# {\"type\": \"Method-Configuration\"}\ninspectit:\n  instrumentation:\n    scopes:\n      's_apacheclient_doExecute':\n        interfaces:\n          - name: 'org.apache.http.impl.client.CloseableHttpClient'\n        methods:\n          - name: 'doExecute'\n      \n      's_httpurlconnection_getOutputStream':\n        type:\n          name: 'java.net.HttpURLConnection'\n        methods:\n          - name: 'getOutputStream'\n            arguments: []\n\n      's_httpurlconnection_getInputStream':\n        superclass:\n          name: 'java.net.HttpURLConnection'\n        methods:\n          - name: 'getInputStream'\n            arguments: []\n\n      's_httpurlconnection_connect':\n        superclass:\n          name: 'java.net.HttpURLConnection'\n        methods:\n          - name: 'connect'\n            arguments: []\n\n      's_httpurlconnection_requestInitiators':\n        superclass:\n          name: 'java.net.HttpURLConnection'\n        methods:\n          - name: 'getHeaderField'\n          - name: 'getHeaderFieldDate'\n          - name: 'getHeaderFieldKey'\n          - name: 'getResponseCode'\n          - name: 'getResponseMessage'",
};

export const Empty = Template.bind({});
Empty.args = {};

export const InvalidConfiguration = Template.bind({});
InvalidConfiguration.args = {
  yamlConfiguration: '# {"type": "Method-Configuration"}\ninspectit: INVALID\n    thread-pool-size: 5',
};
