import * as types from "./types";
import axios from '../../../lib/axios-api';
import { BASE_API_URL_V1 } from '../../../data/constants';
import { authenticationActions } from '../authentication'

/**
 * Fetches all existing configuration files and directories.
 */
export const fetchFiles = () => {
    return (dispatch, state) => {
        dispatch(fetchFilesStarted());

        const { token } = state().authentication;
        var config = {
            headers: { 'Authorization': "Bearer " + token }
        };

        axios
            .get(BASE_API_URL_V1 + "/directories/", config)
            .then(res => {
                const files = res.data;
                dispatch(fetchFilesSuccess(files));
            })
            .catch(err => {
                const { response, message } = err;
                if (response && response.status == 401) {
                    dispatch(authenticationActions.unauthorizedResponse());
                }

                dispatch(fetchFilesFailure(message));
            });
    };
};


/**
 * Is dispatched when the fetching of the configuration files has been started.
 */
export const fetchFilesStarted = () => ({
    type: types.FETCH_FILES_STARTED
});

/**
 * Is dispatched if the fetching of the configuration files was not successful.
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
 * Is dispatched when the fetching of the configuration files was successful.
 * 
 * @param {*} files - the fetched files
 */
export const fetchFilesSuccess = (files) => ({
    type: types.FETCH_FILES_SUCCESS,
    payload: {
        files
    }
});

/**
 * Sets the selection to the given file.
 * 
 * @param {string} selection - absolute path of the selected file (e.g. /configs/prod/interfaces.yml)
 */
export const selectFile = (selection) => ({
    type: types.SELECT_FILE,
    payload: {
        selection
    }
});

/**
 * Resets the configuration state.
 */
export const resetState = () => ({
    type: types.RESET
});