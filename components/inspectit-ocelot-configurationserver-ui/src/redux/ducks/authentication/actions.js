import * as types from "./types";
import axios from '../../../lib/axios-api';

export const fetchToken = (username, password) => {
    return dispatch => {
        dispatch(fetchTokenStarted());

        axios
            .get("http://localhost:8090/api/v1/account/token", {
                auth: {
                    username: username,
                    password: password
                }
            })
            .then(res => {
                const token = res.data;
                dispatch(fetchTokenSuccess(token));
            })
            .catch(err => {
                let message;
                const {response} = err;
                if (response && response.status == 401) {
                    message = "The given credentials are not valid.";
                } else {
                    message = err.message;
                }
                dispatch(fetchTokenFailure(message));
            });
    };
};

export const fetchTokenStarted = () => ({
    type: types.FETCH_TOKEN_STARTED
});

export const fetchTokenFailure = (error) => ({
    type: types.FETCH_TOKEN_FAILURE,
    payload: {
        error
    }
});

export const fetchTokenSuccess = (token) => ({
    type: types.FETCH_TOKEN_SUCCESS,
    payload: {
        token
    }
});