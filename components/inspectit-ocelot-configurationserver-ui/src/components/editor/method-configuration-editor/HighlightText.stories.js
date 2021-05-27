import React from 'react';

import HighlightText from './HighlightText';

export default {
  title: 'Components/Editor/MethodConfigurationEditor/HighlightText',
  component: HighlightText,
};

const Template = (args) => <HighlightText {...args} />;

export const Default = Template.bind({});
Default.args = {
    value: "Hello World!"
};

export const Blue = Template.bind({});
Blue.args = {
    value: "Hello World!",
    theme: "blue"
};
