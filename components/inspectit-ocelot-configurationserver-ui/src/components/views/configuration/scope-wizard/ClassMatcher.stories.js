import React from 'react';

import ClassMatcher from './ClassMatcher';

export default {
  title: 'Components/Views/Configuration/Scope-Wizard/ClassMatcher',
  component: ClassMatcher,
};

const Template = (args) => <ClassMatcher {...args} />;

export const Default = Template.bind({});
Default.args = {
  classMatcher: { type: 'class', matcherType: 'EQUALS_FULLY', className: '' },
};

export const Prefilled = Template.bind({});
Prefilled.args = {
  classMatcher: { type: 'superclass', matcherType: 'STARTS_WITH_IGNORE_CASE', name: 'java.net.HttpURLConnection' },
};
