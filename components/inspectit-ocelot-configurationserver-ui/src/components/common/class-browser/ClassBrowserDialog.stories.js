import React from 'react';

import ClassBrowserDialog from './ClassBrowserDialog';

export default {
  title: 'Components/Common/ClassBrowser/ClassBrowserDialog',
  component: ClassBrowserDialog,
};

const Template = (args) => <ClassBrowserDialog {...args} />;

export const IsShown = Template.bind({});
IsShown.args = {
  visible: true,
};

export const IsHidden = Template.bind({});
IsHidden.args = {
  visible: false,
};
