import React from 'react';
import _ from 'lodash';

import MethodMatcher from './MethodMatcher';

/** data */
import { methodVisibility } from './ScopeWizardConstants';

export default {
  title: 'Components/Views/Configuration/ScopeWizard/MethodMatcher',
  component: MethodMatcher,
};

const Template = (args) => <MethodMatcher {...args} />;

export const IsShown = Template.bind({});
IsShown.args = {
  methodMatcher: {
    selectedMethodVisibilities: _.clone(methodVisibility),
    methodMatcherType: '',
    isConstructor: 'false',
    isSelectedParameter: false,
    parameterInput: '',
    parameterList: [],
    methodName: '',
  },
};
