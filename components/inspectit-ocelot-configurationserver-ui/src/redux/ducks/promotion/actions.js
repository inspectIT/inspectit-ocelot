import * as types from './types';
import axios from '../../../lib/axios-api';
import { notificationActions } from '../notification';
import { dialogActions } from '../dialog';

/**
 * Fetches the promotions from the server.
 */
export const fetchPromotions = () => {
  return (dispatch) => {
    dispatch({ type: types.FETCH_PROMOTIONS_STARTED });

    const params = {
      'include-content': 'true',
    };

    axios
      .get('/configuration/promotions', { params })
      .then((response) => {
        const promotions = response.data;
        dispatch({ type: types.FETCH_PROMOTIONS_SUCCESS, payload: { promotions } });
      })
      .catch(() => {
        dispatch({ type: types.FETCH_PROMOTIONS_FAILURE });
      });
  };
};

export const setCurrentSelection = (filename) => {
  return (dispatch) => {
    dispatch({ type: types.SET_CURRENT_SELECTION, payload: { filename } });
  };
};

export const approveFile = (file) => {
  return (dispatch) => {
    dispatch({ type: types.APPROVE_FILE, payload: { file } });
  };
};

export const disapproveFile = (file) => {
  return (dispatch) => {
    dispatch({ type: types.DISAPPROVE_FILE, payload: { file } });
  };
};

export const promoteConfiguration = () => {
  return (dispatch, getState) => {
    dispatch({ type: types.PROMOTE_CONFIGURATION_STARTED });

    const { files, workspaceCommitId, liveCommitId } = getState().promotion;

    const promotionFiles = _(files).filter({ approved: true }).map('file').value();

    const payload = {
      files: promotionFiles,
      workspaceCommitId,
      liveCommitId,
    };

    axios
      .post('/configuration/promote', payload)
      .then((response) => {
        dispatch({ type: types.PROMOTE_CONFIGURATION_SUCCESS });
        dispatch(notificationActions.showSuccessMessage('Promotion Successful', 'The approved configuration files have been promoted.'));
        dispatch(fetchPromotions());
      })
      .catch(() => {
        dispatch({ type: types.PROMOTE_CONFIGURATION_FAILURE });
        dispatch(dialogActions.showPromotionConflictDialog());
      });

    dispatch({ type: types.PROMOTE_CONFIGURATION_SUCCESS });
  };
};
