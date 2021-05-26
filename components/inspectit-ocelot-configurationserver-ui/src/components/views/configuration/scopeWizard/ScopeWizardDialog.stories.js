import React from 'react';

import ScopeWizardDialog from './ScopeWizardDialog';

export default {
  title: 'Components/Views/Configuration/ScopeWizard/ScopeWizardDialog',
  component: ScopeWizardDialog,
};

const Template = (args) => <ScopeWizardDialog {...args} />;

export const IsShown = Template.bind({});
IsShown.args = {
  visible: true,
};

export const IsHidden = Template.bind({});
IsHidden.args = {
  visible: false,
};
