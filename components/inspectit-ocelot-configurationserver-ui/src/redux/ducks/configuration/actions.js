import * as types from "./types";
import axios from '../../../lib/axios-api';

/**
 * Fetches all existing configuration files and directories.
 */
export const fetchFiles = () => {
    return dispatch => {
        dispatch(fetchFilesStarted());

        axios
            .get("/directories/")
            .then(res => {
                const files = res.data;
                dispatch(fetchFilesSuccess(files));
            })
            .catch(() => {
                dispatch(fetchFilesFailure());
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
 */
export const fetchFilesFailure = () => ({
    type: types.FETCH_FILES_FAILURE
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