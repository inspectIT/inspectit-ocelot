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
});

export default alertingReducer;
