import React from 'react';

import TypeMatcher from './TypeMatcher';

export default {
  title: 'Components/Views/Configuration/Scope-Wizard/TypeMatcher',
  component: TypeMatcher,
};

const Template = (args) => <TypeMatcher {...args} />;

export const Default = Template.bind({});
Default.args = {
  typeMatcher: { type: 'type', matcherType: 'EQUALS_FULLY', name: '' },
};

export const Prefilled = Template.bind({});
Prefilled.args = {
  typeMatcher: { type: 'superclass', matcherType: 'STARTS_WITH_IGNORE_CASE', name: 'java.net.HttpURLConnection' },
};
