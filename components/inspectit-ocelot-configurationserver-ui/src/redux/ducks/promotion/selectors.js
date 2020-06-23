import { createSelector } from 'reselect';
import _ from 'lodash';

const promotionSelector = (state) => state.promotion;

/**
 * Returns the currently selected file - not the filename but the whole object.
 */
export const getCurrentSelectionFile = createSelector(promotionSelector, (promotion) => {
  return _.find(promotion.files, { file: promotion.currentSelection });
});

/**
 * Returns the amount of approved files.
 */
export const getApprovalCount = createSelector(promotionSelector, (promotion) => {
  return _(promotion.files).filter({ approved: true }).value().length;
});

/**
 * Returns the amount of promotion files.
 */
export const getFileCount = createSelector(promotionSelector, (promotion) => {
  return Array.isArray(promotion.files) ? promotion.files.length : 0;
});
