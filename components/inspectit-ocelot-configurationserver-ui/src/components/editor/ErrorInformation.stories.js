import React from 'react';

import ErrorInformation from './ErrorInformation';

export default {
  title: 'Components/Editor/ErrorInformation',
  component: ErrorInformation,
};

const Template = (args) => <ErrorInformation {...args} />;

export const Default = Template.bind({});
Default.args = {
  text: 'Hello World.',
  error: new Error('Whoops!')
};
