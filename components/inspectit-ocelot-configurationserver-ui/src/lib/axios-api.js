import axios from "axios";
import { getStore } from './with-redux-store';
import { authenticationActions } from '../redux/ducks/authentication';
import { notificationActions } from '../redux/ducks/notification';

const isProduction = process.env.NODE_ENV === 'production';
const BASE_API_URL_V1 = isProduction ? "/api/v1" : "http://localhost:8090/api/v1";

const commonConfiguration = {
    baseURL: BASE_API_URL_V1,
    timeout: 5000
};

const axiosPlain = axios.create(commonConfiguration);
const axiosBearer = axios.create(commonConfiguration);

// Request Interceptors
axiosBearer.interceptors.request.use(function (config) {
    const state = getStore().getState();
    const { token } = state.authentication;

    config.headers.authorization = "Bearer " + token;

    return config;
}, function (error) {
    return Promise.reject(error);
});

// Response Interceptors
axiosBearer.interceptors.response.use(function (response) {
    return response;
}, function (error) {
    const reduxStore = getStore();

    if (error.response) {
        const { response: { status } } = error;
        if (status == 401) {
            reduxStore.dispatch(notificationActions.showWarningMessage("Unauthorized", "Your access token is no longer valid. Please login again."));
            reduxStore.dispatch(authenticationActions.logout());
        }
    } else {
        const { message } = error;
        reduxStore.dispatch(notificationActions.showErrorMessage("Request failed", message));
    }

    return Promise.reject(error);
});

export {
    axiosPlain
};

export default axiosBearer;