import * as types from "./types";
import axios from '../../../lib/axios-api';
import { BASE_API_URL_V1 } from '../../../data/constants';

export const fetchFiles = (fileRoot = "/") => {
    return (dispatch, state) => {
        dispatch(fetchFilesStarted());

        const {token} = state().authentication;
        var config = {
            headers: {'Authorization': "Bearer " + token}
        };

        axios
            .get(BASE_API_URL_V1 + "/directories" + fileRoot, config)
            .then(res => {
                const files = res.data;
                dispatch(fetchFilesSuccess(fileRoot, files));
            })
            .catch(err => {
                let message;
                const { response } = err;
                if (response && response.status == 401) {
                    message = "The given credentials are not valid.";
                } else {
                    message = err.message;
                }
                dispatch(fetchFilesFailure(message));
            });
    };
};


/**
 * Is dispatched when the fetching of the access token has been started.
 */
export const fetchFilesStarted = () => ({
    type: types.FETCH_FILES_STARTED
});

/**
 * Is dispatched if the fetching of the access token was not successful.
 * 
 * @param {*} error 
 */
export const fetchFilesFailure = (error) => ({
    type: types.FETCH_FILES_FAILURE,
    payload: {
        error
    }
});

/**
 * Is dispatched when the fetching of the access token was successful.
 * 
 * @param {string} token 
 */
export const fetchFilesSuccess = (fileRoot, files) => ({
    type: types.FETCH_FILES_SUCCESS,
    payload: {
        fileRoot,
        files
    }
});

/**
 * Is dispatched when the fetching of the access token has been started.
 */
export const selectFile = (selection) => ({
    type: types.SELECT_FILE,
    payload: {
        selection
    }
});