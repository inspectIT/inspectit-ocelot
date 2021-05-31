import React from 'react';

import ConvertDialog from './ConvertDialog';

export default {
  title: 'Components/Editor/Dialogs/ConvertDialog',
  component: ConvertDialog,
};

const Template = (args) => <ConvertDialog {...args} />;

export const IsShown = Template.bind({});
IsShown.args = {
  name: 'my-file.yml',
  text: 'Warning',
  visible: true,
};

export const IsHidden = Template.bind({});
IsHidden.args = {
  visible: false,
};
