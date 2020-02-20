import * as types from "./types";
import * as selectors from "./selectors";
import axios from '../../../lib/axios-api';
import { configurationUtils } from '../configuration';
import { notificationActions } from '../notification';
import { DEFAULT_CONFIG_TREE_KEY } from '../../../data/constants';

/**
 * Fetches all existing configuration files and directories.
 */
export const fetchFiles = () => {
    return dispatch => {
        dispatch({ type: types.FETCH_FILES_STARTED });

        axios
            .get("/directories/")
            .then(res => {
                const files = res.data;
                dispatch({ type: types.FETCH_FILES_SUCCESS, payload: { files } });
            })
            .catch(() => {
                dispatch({ type: types.FETCH_FILES_FAILURE });
            });
    };
};

/**
 * Fetches the content of the selected file.
 */
export const fetchSelectedFile = () => {
    return (dispatch, getState) => {
        const { selection } = getState().configuration;

        if (selection) {
            const file = configurationUtils.getFile(getState().configuration.files, selection);
            const isDirectory = configurationUtils.isDirectory(file);

            if (!isDirectory) {
                dispatch({ type: types.FETCH_FILE_STARTED });

                axios
                    .get("/files" + selection)
                    .then(res => {
                        const fileContent = res.data.content;
                        dispatch({ type: types.FETCH_FILE_SUCCESS, payload: { fileContent } });
                    })
                    .catch(() => {
                        dispatch({ type: types.FETCH_FILE_FAILURE });
                    });
            }
        }
    };
};

/**
 * Sets the selection to the given file.
 * 
 * @param {string} selection - absolute path of the selected file (e.g. /configs/prod/interfaces.yml)
 */
export const selectFile = (selection) => {
    return (dispatch, getState) => {
        if (selection && selection.startsWith(DEFAULT_CONFIG_TREE_KEY)) {
            const content = configurationUtils.getDefaultFileContent(getState().configuration.defaultConfig, selection);
            dispatch({
                type: types.SELECT_DEFAULT_CONFIG_FILE,
                payload: {
                    selection,
                    content
                }
            });
        } else {
            dispatch({
                type: types.SELECT_FILE,
                payload: {
                    selection
                }
            });

            dispatch(fetchSelectedFile(selection));
        }
    };
};

/**
 * Resets the configuration state.
 */
export const resetState = () => ({
    type: types.RESET
});

/**
 * Attempts to delete the currently selected or handed in file or folder.
 * In case of success, fetchFiles() is automatically triggered.
 */
export const deleteSelection = (fetchFilesOnSuccess, selectedFile = null) => {
    return (dispatch, getState) => {
        const state = getState();
        const { selection, files } = state.configuration;

        const selectedName = selectedFile ? selectedFile : selection;

        const file = configurationUtils.getFile(files, selectedName);
        const isDirectory = configurationUtils.isDirectory(file);

        let filePath = selectedName && selectedName.startsWith("/") ? selectedName.substring(1) : selectedName;

        dispatch({ type: types.DELETE_SELECTION_STARTED });

        axios
            .delete((isDirectory ? "/directories/" : "/files/") + filePath)
            .then(res => {
                dispatch({ type: types.DELETE_SELECTION_SUCCESS });
                if (fetchFilesOnSuccess) {
                    dispatch(fetchFiles());
                }
            })
            .catch((error) => {
                dispatch({ type: types.DELETE_SELECTION_FAILURE });
            });
    };
};


/**
 * Attempts to write the given contents to the given file.
 * Triggers fetchFiles() if requested on success.
 */
export const writeFile = (file, content, fetchFilesOnSuccess) => {
    return (dispatch, getState) => {

        let filePath = file.startsWith("/") ? file.substring(1) : file;

        dispatch({ type: types.WRITE_FILE_STARTED });

        axios
            .put("/files/" + filePath, {
                content
            })
            .then(res => {
                const payload = {
                    file,
                    content
                };

                dispatch({ type: types.WRITE_FILE_SUCCESS, payload });

                if (fetchFilesOnSuccess) {
                    dispatch(fetchFiles());
                }

                dispatch(notificationActions.showSuccessMessage("Configuration Saved", "The configuration has been successfully saved."));
            })
            .catch((error) => {
                dispatch({ type: types.WRITE_FILE_FAILURE });
            });
    };
};


/**
 * Attempts to create the given directory.
 * Triggers fetchFiles() if requested on success.
 */
export const createDirectory = (path, fetchFilesOnSuccess) => {
    return (dispatch) => {

        let dirPath = path.startsWith("/") ? path.substring(1) : path;

        dispatch({ type: types.CREATE_DIRECTORY_STARTED });

        axios
            .put("/directories/" + dirPath)
            .then(res => {
                dispatch({ type: types.CREATE_DIRECTORY_SUCCESS });
                if (fetchFilesOnSuccess) {
                    dispatch(fetchFiles());
                }
            })
            .catch((error) => {
                dispatch({ type: types.CREATE_DIRECTORY_FAILURE });
            });
    };
};


/**
 * Attempts to move the given directory or file.
 */
export const move = (path, targetPath, fetchFilesOnSuccess) => {
    return (dispatch) => {

        dispatch({ type: types.MOVE_STARTED });
        let payload = {
            source: path,
            target: targetPath
        };
        axios
            .put("/move", payload)
            .then(res => {
                dispatch({
                    type: types.MOVE_SUCCESS,
                    payload,
                });
                if (fetchFilesOnSuccess) {
                    dispatch(fetchFiles());
                }
            })
            .catch((error) => {
                dispatch({ type: types.MOVE_FAILURE });
            });
    };
};

/**
 * Persists unsaved changes for the selected file in the browser if required.
 */
export const selectedFileContentsChanged = (content) => ({
    type: types.SELECTED_FILE_CONTENTS_CHANGED,
    payload: {
        content
    }
});

/**
 * Fetches the default configuration of the Ocelot agents.
 */
export const fetchDefaultConfig = () => {
    return dispatch => {
        dispatch({ type: types.FETCH_DEFAULT_CONFIG_STARTED });

        axios
            .get("/defaultconfig")
            .then(res => {
                const defaultConfig = res.data;
                dispatch({ type: types.FETCH_DEFAULT_CONFIG_SUCCESS, payload: { defaultConfig } });
            })
            .catch(() => {
                dispatch({ type: types.FETCH_DEFAULT_CONFIG_FAILURE });
            })
    }
}

/**
 * Fetches the configuration schema.
 */
export const fetchConfigurationSchema= () => {
    return dispatch => {
        dispatch({ type: types.FETCH_SCHEMA_STARTED });

        axios
            .get("/schema/plain")
            .then(res => {
                const schema = res.data;
                dispatch({ type: types.FETCH_SCHEMA_SUCCESS, payload: { schema } });
            })
            .catch(() => {
                dispatch({ type: types.FETCH_SCHEMA_FAILURE });
            });
    };
};