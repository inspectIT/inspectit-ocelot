import React from 'react';

import ScopeTypeDisplay from './ScopeTypeDisplay';

export default {
  title: 'Components/Editor/MethodConfigurationEditor/ScopeTypeDisplay',
  component: ScopeTypeDisplay,
};

const Template = (args) => <ScopeTypeDisplay {...args} />;

export const ShowSuperclass = Template.bind({});
ShowSuperclass.args = {
  scope: { superclass: { name: 'java.net.HttpURLConnection' }, methods: [{ name: 'getInputStream', arguments: [] }] },
};

export const ShowType = Template.bind({});
ShowType.args = {
  scope: { type: { name: 'org.apache.http.impl.client.CloseableHttpClient' }, methods: [{ name: 'getInputStream', arguments: [] }] },
};

export const ShowInterface = Template.bind({});
ShowInterface.args = {
  scope: { interfaces: [{ name: 'org.apache.http.impl.client.CloseableHttpClient' }], methods: [{ name: 'getInputStream', arguments: [] }] },
};
