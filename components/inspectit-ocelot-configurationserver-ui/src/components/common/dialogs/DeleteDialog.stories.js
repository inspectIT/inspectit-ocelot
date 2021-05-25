import React from 'react';

import DeleteDialog from './DeleteDialog';

export default {
  title: 'Components/Common/Dialogs/DeleteDialog',
  component: DeleteDialog,
};

const Template = (args) => <DeleteDialog {...args} />;

export const IsShown = Template.bind({});
IsShown.args = {
    name: 'Any Element',
    text: 'Dialog Title',
    visible: true
};

export const IsHidden = Template.bind({});
IsHidden.args = {
    visible: false
};