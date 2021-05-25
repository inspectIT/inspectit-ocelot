import React from 'react';

import EditorToolbar from './EditorToolbar';

export default {
  title: 'Components/Editor/EditorToolbar',
  component: EditorToolbar,
};

const Template = (args) => <EditorToolbar {...args} />;

export const InYamlEditor = Template.bind({});
InYamlEditor.args = {
    enableButtons: true,
    isRefreshing: false,
    canSave: true,
    visualConfig: false,
    children: <div>Child Component</div>,
};

export const DisabledButtons = Template.bind({});
DisabledButtons.args = {
    enableButtons: false,
    isRefreshing: false,
    canSave: false,
    visualConfig: false,
    children: <div>Child Component</div>,
};

export const IsLoading = Template.bind({});
IsLoading.args = {
    enableButtons: true,
    isRefreshing: true,
    canSave: false,
    visualConfig: false,
    children: <div>Child Component</div>,
};