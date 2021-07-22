import React from 'react';

import WarningDialog from './WarningDialog';

export default {
  title: 'Components/Common/Dialogs/WarningDialog',
  component: WarningDialog,
};

const Template = (args) => (
  <WarningDialog {...args}>
    <p>Try not. Do or do not. There is no try.</p>
    <p>-- Yoda</p>
  </WarningDialog>
);

export const IsShown = Template.bind({});
IsShown.args = {
  visible: true,
  title: 'Hello Warning',
};

export const IsHidden = Template.bind({});
IsHidden.args = {
  visible: false,
};
