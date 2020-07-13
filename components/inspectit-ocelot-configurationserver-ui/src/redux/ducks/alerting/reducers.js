import { createReducer } from '../../utils';
import { alerting as initialState } from '../initial-states';
import * as types from './types';

const alertingReducer = createReducer(initialState)({
  [types.UNSAVED_RULE_CONTENTS_CHANGED]: (state, action) => {
    const { unsavedRuleContentsMap } = action.payload;

    return {
      ...state,
      unsavedRuleContents: unsavedRuleContentsMap,
    };
  },
  [types.ALERTING_RULES_GROUPING_CONFIG_CHANGED]: (state, action) => {
    const { groupingOptions } = action.payload;

    return {
      ...state,
      ruleGrouping: groupingOptions,
    };
  },
  [types.UNSAVED_HANDLER_CONTENTS_CHANGED]: (state, action) => {
    const { unsavedHandlerContentsMap } = action.payload;

    return {
      ...state,
      unsavedHandlerContents: unsavedHandlerContentsMap,
    };
  },
});

export default alertingReducer;
