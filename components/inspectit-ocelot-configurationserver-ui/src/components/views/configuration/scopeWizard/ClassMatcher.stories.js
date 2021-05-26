import React from 'react';

import ClassMatcher from './ClassMatcher';

export default {
  title: 'Components/Views/Configuration/ScopeWizard/ClassMatcher',
  component: ClassMatcher,
};

const Template = (args) => <ClassMatcher {...args} />;

export const IsShown = Template.bind({});
IsShown.args = {
  classMatcher: { currentClassMatcher: '', classMatcherType: '', className: '' },
};

// export const IsHidden = Template.bind({});
// IsHidden.args = {
//   visible: false,
// };