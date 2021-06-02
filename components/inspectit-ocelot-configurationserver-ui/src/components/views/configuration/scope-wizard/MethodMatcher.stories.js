import React from 'react';
import _ from 'lodash';

import MethodMatcher from './MethodMatcher';

/** data */
import { METHOD_VISIBILITY } from './ScopeWizardConstants';

export default {
  title: 'Components/Views/Configuration/Scope-Wizard/MethodMatcher',
  component: MethodMatcher,
};

const Template = (args) => <MethodMatcher {...args} />;

export const Default = Template.bind({});
Default.args = {
  methodMatcher: {
    visibilities: _.clone(METHOD_VISIBILITY),
    matcherType: null,
    isConstructor: 'false',
    isSelectedParameter: false,
    parameterInput: null,
    parameterList: [],
    name: null,
  },
};

export const MethodName = Template.bind({});
MethodName.args = {
  methodMatcher: {
    visibilities: _.clone(METHOD_VISIBILITY),
    matcherType: 'EQUALS_FULLY',
    isConstructor: false,
    isSelectedParameter: true,
    parameterInput: null,
    parameterList: [{ parameter: 'java.lang.Object' }],
    name: 'doExecute',
  },
};

export const Constructor = Template.bind({});
Constructor.args = {
  methodMatcher: {
    visibilities: _.clone(METHOD_VISIBILITY),
    matcherType: null,
    isConstructor: true,
    isSelectedParameter: false,
    parameterInput: null,
    parameterList: [{ parameter: 'java.lang.Object' }],
    name: 'doExecute',
  },
};
