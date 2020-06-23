import * as types from './types';
import axios from '../../../lib/axios-api';
import { notificationActions } from '../notification';
import { dialogActions } from '../dialog';
import { PROMOTION_CONFLICT_DIALOG } from '../../../components/dialogs/dialogs';
import _ from 'lodash';

/**
 * Fetches the promotion files from the server.
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

/**
 * Sets the current selected promotion file
 *
 * @param {string} filename
 */
export const setCurrentSelection = (filename) => {
  return (dispatch) => {
    dispatch({ type: types.SET_CURRENT_SELECTION, payload: { filename } });
  };
};

/**
 * Approve the given file and marks it ready for promotion.
 *
 * @param {string} filename
 */
export const approveFile = (filename) => {
  return (dispatch) => {
    dispatch({ type: types.APPROVE_FILE, payload: { filename } });
  };
};

/**
 * Disapprove the given file, so it will not be promoted.
 *
 * @param {string} filename
 */
export const disapproveFile = (filename) => {
  return (dispatch) => {
    dispatch({ type: types.DISAPPROVE_FILE, payload: { filename } });
  };
};

/**
 * Trigger a configuration promotion.
 */
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
      .then(() => {
        dispatch({ type: types.PROMOTE_CONFIGURATION_SUCCESS });
        dispatch(notificationActions.showSuccessMessage('Promotion Successful', 'The approved configuration files have been promoted.'));
        dispatch(fetchPromotions());
      })
      .catch(() => {
        dispatch({ type: types.PROMOTE_CONFIGURATION_FAILURE });
        dispatch(dialogActions.showDialog(PROMOTION_CONFLICT_DIALOG));
      });

    dispatch({ type: types.PROMOTE_CONFIGURATION_SUCCESS });
  };
};
