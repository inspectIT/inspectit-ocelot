import React from 'react';

import Notificationbar from './Notificationbar';

export default {
  title: 'Components/Editor/Notificationbar',
  component: Notificationbar,
};

const Template = (args) => <Notificationbar {...args} />;

export const ShowMessage = Template.bind({});
ShowMessage.args = {
  isError: false,
  icon: null,
  text: 'Hello World.',
};

export const ShowsError = Template.bind({});
ShowsError.args = {
  isError: true,
  icon: 'pi-exclamation-triangle',
  text: 'This is an error.',
};
