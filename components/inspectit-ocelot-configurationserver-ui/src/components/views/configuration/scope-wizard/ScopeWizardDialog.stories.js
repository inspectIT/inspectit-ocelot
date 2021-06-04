import React from 'react';

import ScopeWizardDialog from './ScopeWizardDialog';

export default {
  title: 'Components/Views/Configuration/Scope-Wizard/ScopeWizardDialog',
  component: ScopeWizardDialog,
};

const Template = (args) => <ScopeWizardDialog {...args} />;

export const IsShown = Template.bind({});
IsShown.args = {
  visible: true,
  scope: null,
};

export const IsHidden = Template.bind({});
IsHidden.args = {
  visible: false,
  scope: null,
};

export const EditMode = Template.bind({});
EditMode.args = {
  visible: true,
  scope: {
    type: { name: 'myClass', 'matcher-mode': 'EQUALS_FULLY' },
    methods: [{ name: 'myMethod', 'matcher-mode': 'MATCHES', visibility: ['PROTECTED', 'PRIVATE'], arguments: ['firstArg', 'secondArg'] }],
  },
};
