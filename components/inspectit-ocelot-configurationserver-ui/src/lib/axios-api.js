import axios from 'axios';
import { getStore } from './with-redux-store';
import { authenticationActions } from '../redux/ducks/authentication';
import { notificationActions } from '../redux/ducks/notification';

const isProduction = process.env.NODE_ENV === 'production';
const BASE_API_URL_V1 = isProduction ? '/api/v1' : 'http://localhost:8090/api/v1';

const commonConfiguration = {
  baseURL: BASE_API_URL_V1,
  timeout: 5000,
};

const axiosPlain = axios.create(commonConfiguration);
const axiosBearer = axios.create(commonConfiguration);

// ############################################################################
// Request Interceptors

/**
 * Ensures requres are authenticated using the bearer token.
 */
axiosBearer.interceptors.request.use(
  function (config) {
    const state = getStore().getState();
    const { token } = state.authentication;

    config.headers.authorization = 'Bearer ' + token;

    return config;
  },
  function (error) {
    return Promise.reject(error);
  }
);

// ############################################################################
// Response Interceptors

/**
 * Does error handling. Shows notifications in case an error occurs or triggers
 * a logout if a request returns with a 401 status code (unauthorized).
 */
axiosBearer.interceptors.response.use(
  function (response) {
    return response;
  },
  function (error) {
    const reduxStore = getStore();

    if (error.response && error.response.status === 401) {
      reduxStore.dispatch(
        notificationActions.showWarningMessage('Unauthorized', 'Your access token is no longer valid. Please login again.')
      );
      reduxStore.dispatch(authenticationActions.logout());
    } else if (error.response && error.response.data && error.response.data.message) {
      const { message } = error.response.data;
      reduxStore.dispatch(notificationActions.showErrorMessage('Request failed', message));
    } else {
      const { message } = error;
      reduxStore.dispatch(notificationActions.showErrorMessage('Request failed', message));
    }

    return Promise.reject(error);
  }
);

export { axiosPlain };

export default axiosBearer;
