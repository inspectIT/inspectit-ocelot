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
