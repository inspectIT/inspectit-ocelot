import * as types from './types';
import axios from '../../../lib/axios-api';

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

export const setCurrentSelection = (file) => {
  return (dispatch) => {
    dispatch({ type: types.SET_CURRENT_SELECTION, payload: { file } });
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
