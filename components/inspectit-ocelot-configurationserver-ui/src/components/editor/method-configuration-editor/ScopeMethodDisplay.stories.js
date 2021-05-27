import React from 'react';

import ScopeMethodDisplay from './ScopeMethodDisplay';

export default {
  title: 'Components/Editor/MethodConfigurationEditor/ScopeMethodDisplay',
  component: ScopeMethodDisplay,
};

const Template = (args) => <ScopeMethodDisplay {...args} />;

export const AllMethods = Template.bind({});
AllMethods.args = {
  scope: { superclass: { name: 'java.net.HttpURLConnection', 'MATCHER-MODE': 'ENDS_WITH_IGNORE_CASE' }, methods: [] },
};

export const OnlyName = Template.bind({});
OnlyName.args = {
  scope: {
    superclass: { name: 'java.net.HttpURLConnection', 'MATCHER-MODE': 'ENDS_WITH_IGNORE_CASE' },
    methods: [{ name: 'getInputStream' }],
  },
};

export const NameMatches = Template.bind({});
NameMatches.args = {
  scope: {
    type: { name: 'org.apache.http.impl.client.CloseableHttpClient', 'Matcher-Mode': 'MATCHES' },
    methods: [{ name: 'getInputStream', 'Matcher-Mode': 'STARTS_WITH_IGNORE_CASE' }],
  },
};

export const ByVisibility = Template.bind({});
ByVisibility.args = {
  scope: {
    interfaces: [{ name: 'org.apache.http.impl.client.CloseableHttpClient' }],
    methods: [{ visibility: ['PUBLIC', 'PRIVATE'] }],
  },
};

export const AllVisibilities = Template.bind({});
AllVisibilities.args = {
  scope: {
    interfaces: [{ name: 'org.apache.http.impl.client.CloseableHttpClient' }],
    methods: [{ name: 'getInputStream', visibility: ['PUBLIC', 'PRIVATE', 'PROTECTED', 'PACKAGE'] }],
  },
};

export const WithArguments = Template.bind({});
WithArguments.args = {
  scope: {
    interfaces: [{ name: 'org.apache.http.impl.client.CloseableHttpClient' }],
    methods: [{ name: 'getInputStream', arguments: ['java.lang.Object', 'java.lang.Object'] }],
  },
};

export const WithoutArguments = Template.bind({});
WithoutArguments.args = {
  scope: {
    interfaces: [{ name: 'org.apache.http.impl.client.CloseableHttpClient' }],
    methods: [{ name: 'getInputStream', arguments: [] }],
  },
};

export const IsConstructor = Template.bind({});
IsConstructor.args = {
  scope: {
    interfaces: [{ name: 'org.apache.http.impl.client.CloseableHttpClient' }],
    methods: [{ 'is-constructor': true, arguments: [], visibility: ['PUBLIC'] }],
  },
};
