import { createSelector } from 'reselect';

const alertingSelector = (state) => state.alerting;

/**
 * Returns true if there are any unsaved alerting changes.
 */
export const hasUnsavedChanges = createSelector(alertingSelector, (alerting) => {
  const { unsavedRuleContents, unsavedHandlerContents } = alerting;
  return Object.keys(unsavedRuleContents).length > 0 || Object.keys(unsavedHandlerContents).length > 0;
});
