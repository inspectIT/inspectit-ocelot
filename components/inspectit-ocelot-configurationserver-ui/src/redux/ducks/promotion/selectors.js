import { createSelector } from 'reselect';
import _ from 'lodash';

const promotionSelector = (state) => state.promotion;

export const getCurrentSelectionFile = createSelector(promotionSelector, (promotion) => {
    return _.find(promotion.files, {file: promotion.currentSelection});
  });

  export const getApprovalCount = createSelector(promotionSelector, (promotion) => {
      return _(promotion.files).filter({approved: true}).value().length;;
  });