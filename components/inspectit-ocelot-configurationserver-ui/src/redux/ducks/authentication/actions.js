import * as types from './types';
import axiosBearer, { axiosPlain } from '../../../lib/axios-api';
import axios from 'axios';

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

    const auth = {
      username: username,
      password: password,
    };

    const tokenRequest = axiosPlain.get('/account/token', { auth });
    const permissionsRequest = axiosPlain.get('/account/permissions', { auth });

    axios
      .all([tokenRequest, permissionsRequest])
      .then(
        axios.spread((...responses) => {
          const token = responses[0].data;
          const permissions = responses[1].data;
          dispatch({ type: types.FETCH_TOKEN_SUCCESS, payload: { token, username, permissions } });
        })
      )
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
    const tokenRequest = axiosBearer.get('/account/token');
    const permissionsRequest = axiosBearer.get('/account/permissions');

    axios
      .all([tokenRequest, permissionsRequest])
      .then(
        axios.spread((...responses) => {
          const token = responses[0].data;
          const permissions = responses[1].data;
          dispatch({ type: types.RENEW_TOKEN_SUCCESS, payload: { token, permissions } });
        })
      )
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
