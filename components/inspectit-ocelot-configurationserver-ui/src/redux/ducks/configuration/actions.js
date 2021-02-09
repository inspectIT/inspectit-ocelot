import { DEFAULT_CONFIG_TREE_KEY, VERSION_LIMIT } from '../../../data/constants';
import axios from '../../../lib/axios-api';
import { configurationUtils } from '.';
import { notificationActions } from '../notification';
import * as types from './types';

/**
 * Fetches all existing versions.
 */
export const fetchVersions = () => {
  return (dispatch) => {
    dispatch({ type: types.FETCH_VERSIONS_STARTED });

    const params = {
      limit: VERSION_LIMIT,
    };

    axios('/versions', { params })
      .then((res) => {
        const versions = res.data;
        dispatch({ type: types.FETCH_VERSIONS_SUCCESS, payload: { versions } });
      })
      .catch(() => {
        dispatch({ type: types.FETCH_VERSIONS_FAILURE });
      });
  };
};

/**
 * Fetches all existing configuration files and directories.
 *
 * @param {string} newSelectionOnSuccess - If not empty, this path will be selected on successful fetch.
 */
export const fetchFiles = (newSelectionOnSuccess) => {
  return (dispatch, getState) => {
    const { selectedVersion } = getState().configuration;

    const params = {};
    if (selectedVersion) {
      params.version = selectedVersion;
    }

    dispatch({ type: types.FETCH_FILES_STARTED });
    axios
      .get('/directories', { params })
      .then((res) => {
        const files = res.data;
        sortFiles(files);
        dispatch({ type: types.FETCH_FILES_SUCCESS, payload: { files } });
        if (newSelectionOnSuccess) {
          dispatch(selectFile(newSelectionOnSuccess));
        }
      })
      .catch(() => {
        dispatch({ type: types.FETCH_FILES_FAILURE });
      });
  };
};

/**
 * Arranges first directories and then files. Within the directories or files it is an alphabetical sorting.
 */
const sortFiles = (allFiles) => {
  allFiles.sort((first, second) => {
    if (first.type !== second.type) {
      if (first.type === 'directory') {
        return -1;
      } else {
        return 1;
      }
    }
    const nameFirst = first.name.toUpperCase();
    const nameSecond = second.name.toUpperCase();

    return nameFirst.localeCompare(nameSecond);
  });

  allFiles.forEach((element) => {
    if (element.children) {
      sortFiles(element.children);
    }
  });
};

/**
 * Fetch the content of the selected file from version with selected id.
 */
export const fetchSelectedFile = () => {
  return (dispatch, getState) => {
    const { selection, files, selectedVersion } = getState().configuration;

    if (selection) {
      const file = configurationUtils.getFile(files, selection);
      const isDirectory = configurationUtils.isDirectory(file);

      if (!isDirectory) {
        const params = {};
        if (selectedVersion) {
          params.version = selectedVersion;
        }

        dispatch({ type: types.FETCH_FILE_STARTED });
        axios
          .get('/files' + selection, { params })
          .then((res) => {
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
    if (selection && !selection.startsWith('/')) {
      selection = '/' + selection;
    }

    if (selection && selection.startsWith(DEFAULT_CONFIG_TREE_KEY)) {
      const content = configurationUtils.getDefaultFileContent(getState().configuration.defaultConfig, selection);
      dispatch({
        type: types.SELECT_DEFAULT_CONFIG_FILE,
        payload: {
          selection,
          content,
        },
      });
    } else {
      dispatch({
        type: types.SELECT_FILE,
        payload: {
          selection,
        },
      });
      dispatch(fetchSelectedFile());
    }
  };
};

/**
 * Resets the configuration state.
 */
export const resetState = () => ({
  type: types.RESET,
});

/**
 * Attempts to delete the currently selected or handed in file or folder.
 * In case of success, fetchFiles() is automatically triggered.
 */
export const deleteSelection = (fetchFilesOnSuccess, selectedFile = null) => {
  return (dispatch, getState) => {
    const state = getState();
    const { selection, files } = state.configuration;

    const selectedName = selectedFile || selection;

    const file = configurationUtils.getFile(files, selectedName);
    const isDirectory = configurationUtils.isDirectory(file);

    const filePath = selectedName && selectedName.startsWith('/') ? selectedName.substring(1) : selectedName;

    dispatch({ type: types.DELETE_SELECTION_STARTED });

    axios
      .delete((isDirectory ? '/directories/' : '/files/') + filePath)
      .then(() => {
        dispatch({ type: types.DELETE_SELECTION_SUCCESS });
        if (fetchFilesOnSuccess) {
          dispatch(fetchFiles());
          dispatch(fetchVersions());
        }
      })
      .catch(() => {
        dispatch({ type: types.DELETE_SELECTION_FAILURE });
      });
  };
};

/**
 * Attempts to write the given contents to the given file.
 *
 * @param {string} file - absolute path of the file to write (e.g. /configs/prod/interfaces.yml)
 * @param {string} content - the content to place in the file
 * @param {boolean} fetchFilesOnSuccess - if true, the file tree be refeteched on a successful write
 * @param {boolean} selectFileOnSuccess - if true, the newly created file will be selected on success.
 *                                        Requires fetchFilesOnSuccess to be true
 */
export const writeFile = (file, content, fetchFilesOnSuccess, selectFileOnSuccess) => {
  return (dispatch) => {
    const filePath = file.startsWith('/') ? file.substring(1) : file;

    dispatch({ type: types.WRITE_FILE_STARTED });

    axios
      .put('/files/' + filePath, {
        content,
      })
      .then(() => {
        const payload = {
          file,
          content,
        };

        dispatch({ type: types.WRITE_FILE_SUCCESS, payload });
        dispatch(fetchFiles());
        dispatch(fetchVersions());

        if (fetchFilesOnSuccess) {
          if (selectFileOnSuccess) {
            dispatch('/' + filePath);
          } else {
            dispatch(fetchFiles());
          }
        }
        dispatch(notificationActions.showSuccessMessage('Configuration Saved', 'The configuration has been successfully saved.'));
      })
      .catch(() => {
        dispatch({ type: types.WRITE_FILE_FAILURE });
      });
  };
};

/**
 * Attempts to create the given directory.
 *
 * @param {string} path - absolute path of the directory to create (e.g. /configs/prod/myDir)
 * @param {boolean} fetchFilesOnSuccess - if true, the file tree will be refeteched on a successful creation
 * @param {boolean} selectFolderOnSucces - if true, the newly created directory will be selected on success.
 *                                        Requires fetchFilesOnSuccess to be true
 */
export const createDirectory = (path, fetchFilesOnSuccess, selectFolderOnSuccess) => {
  return (dispatch) => {
    const dirPath = path.startsWith('/') ? path.substring(1) : path;

    dispatch({ type: types.CREATE_DIRECTORY_STARTED });

    axios
      .put('/directories/' + dirPath)
      .then(() => {
        dispatch({ type: types.CREATE_DIRECTORY_SUCCESS });
        if (fetchFilesOnSuccess) {
          if (selectFolderOnSuccess) {
            dispatch(fetchFiles('/' + dirPath));
          } else {
            dispatch(fetchFiles());
          }
        }
        dispatch(fetchSelectedFile());
      })
      .catch(() => {
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
    const payload = {
      source: path,
      target: targetPath,
    };
    axios
      .put('/move', payload)
      .then(() => {
        dispatch({
          type: types.MOVE_SUCCESS,
          payload,
        });
        if (fetchFilesOnSuccess) {
          dispatch(fetchFiles());
          dispatch(fetchVersions());
        }
      })
      .catch(() => {
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
    content,
  },
});

/**
 * Selects the version with the given id.
 */
export const selectVersion = (version, reloadFiles = true) => {
  return (dispatch) => {
    // chaning the selected version
    dispatch({
      type: types.SELECT_VERSION,
      payload: {
        version,
      },
    });

    if (reloadFiles) {
      // fetching the content of the selected version
      dispatch(fetchFiles());
      dispatch(fetchSelectedFile());
    }
  };
};

/**
 * Fetches the default configuration of the Ocelot agents.
 */
export const fetchDefaultConfig = () => {
  return (dispatch) => {
    dispatch({ type: types.FETCH_DEFAULT_CONFIG_STARTED });

    axios
      .get('/defaultconfig')
      .then((res) => {
        const defaultConfig = res.data;
        dispatch({ type: types.FETCH_DEFAULT_CONFIG_SUCCESS, payload: { defaultConfig } });
      })
      .catch(() => {
        dispatch({ type: types.FETCH_DEFAULT_CONFIG_FAILURE });
      });
  };
};

/**
 * Fetches the configuration schema.
 */
export const fetchConfigurationSchema = () => {
  return (dispatch) => {
    dispatch({ type: types.FETCH_SCHEMA_STARTED });

    axios
      .get('/schema/plain')
      .then((res) => {
        const schema = res.data;
        dispatch({ type: types.FETCH_SCHEMA_SUCCESS, payload: { schema } });
      })
      .catch(() => {
        dispatch({ type: types.FETCH_SCHEMA_FAILURE });
      });
  };
};

/**
 * Shows or hides the split view for the configuration properties.
 */
export const toggleVisualConfigurationView = () => ({
  type: types.TOGGLE_VISUAL_CONFIGURATION_VIEW,
});

/**
 * Shows or hides the history voiew.
 */
export const toggleHistoryView = () => ({
  type: types.TOGGLE_HISTORY_VIEW,
});
