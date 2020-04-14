import * as types from './types';
import axiosBearer, { axiosPlain } from '../../../lib/axios-api';

import { configurationActions } from '../configuration';

/**
 * Fetches an access token for the given credentials.
 *
 * @param {string} username
 * @param {string} password
 */
export const fetchToken = (username, password) => {
  return (dispatch) => {
    dispatch({ type: types.FETCH_TOKEN_STARTED });

    axiosPlain
      .get('/account/token', {
        auth: {
          username: username,
          password: password,
        },
      })
      .then((res) => {
        const token = res.data;
        dispatch({ type: types.FETCH_TOKEN_SUCCESS, payload: { token, username } });
      })
      .catch((err) => {
        let message;
        const { response } = err;
        if (response && response.status === 401) {
          message = 'The given credentials are not valid.';
        } else {
          message = err.message;
        }
        dispatch({ type: types.FETCH_TOKEN_FAILURE, payload: { error: message } });
      });
  };
};

/**
 * Renews the access token with the existing token.
 *
 */
export const renewToken = () => {
  return (dispatch) => {
    axiosBearer
      .get('/account/token')
      .then((res) => {
        const token = res.data;
        dispatch({ type: types.RENEW_TOKEN_SUCCESS, payload: { token } });
      })
      .catch(() => {});
  };
};

/**
 * Logout of the current user.
 */
export const logout = () => {
  return (dispatch) => {
    dispatch({ type: types.LOGOUT });
    dispatch(configurationActions.resetState());
  };
};
