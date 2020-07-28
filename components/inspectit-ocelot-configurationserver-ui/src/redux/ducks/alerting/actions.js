import * as types from './types';

/**
 * Persists unsaved changes for the given rule in the browser if required.
 */
export const ruleContentsChanged = (unsavedRuleContentsMap) => ({
  type: types.UNSAVED_RULE_CONTENTS_CHANGED,
  payload: {
    unsavedRuleContentsMap,
  },
});

/**
 * Persists alerting rule grouping options in the browser if required.
 */
export const changeRuleGroupingOptions = (groupingOptions) => ({
  type: types.ALERTING_RULES_GROUPING_CONFIG_CHANGED,
  payload: {
    groupingOptions,
  },
});

/**
 * Persists unsaved changes for the given alert handler in the browser if required.
 */
export const handlerContentsChanged = (unsavedHandlerContentsMap) => ({
  type: types.UNSAVED_HANDLER_CONTENTS_CHANGED,
  payload: {
    unsavedHandlerContentsMap,
  },
});
