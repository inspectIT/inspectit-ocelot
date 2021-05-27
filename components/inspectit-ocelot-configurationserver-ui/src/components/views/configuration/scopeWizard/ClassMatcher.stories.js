import React from 'react';

import ClassMatcher from './ClassMatcher';

export default {
  title: 'Components/Views/Configuration/ScopeWizard/ClassMatcher',
  component: ClassMatcher,
};

const Template = (args) => <ClassMatcher {...args} />;

export const Default = Template.bind({});
Default.args = {
  classMatcher: { currentClassMatcher: '', classMatcherType: '', className: '' },
};
