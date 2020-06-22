import * as types from './types';
import * as dialogs from '../../../components/dialogs/dialogs';

export const hideDialogs = () => {
  return (dispatch) => {
    dispatch({ type: types.HIDE_DIALOG });
  };
};

export const showPromotionApprovalDialog = () => {
  return (dispatch) => showDialog(dispatch, dialogs.PROMOTION_APPROVAL_DIALOG, null);
};

export const showPromotionConflictDialog = () => {
  return (dispatch) => showDialog(dispatch, dialogs.PROMOTION_CONFLICT_DIALOG, null);
};

const showDialog = (dispatch, name, payload) => {
  dispatch({
    type: types.SHOW_DIALOG,
    payload: {
      dialog: name,
      payload,
    },
  });
};
