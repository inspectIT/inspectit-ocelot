import { omitBy } from 'lodash';
import SidebarTypes from '../../../components/views/configuration/SidebarTypes';
import { createReducer } from '../../utils';
import { configuration as initialState } from '../initial-states';
import * as types from './types';
import * as utils from './utils';

const incrementPendingRequests = (state) => {
  return {
    ...state,
    pendingRequests: state.pendingRequests + 1,
  };
};

const decrementPendingRequests = (state) => {
  return {
    ...state,
    pendingRequests: Math.max(0, state.pendingRequests - 1),
  };
};

const movePathIfRequired = (path, moveHistory) => {
  if (!path) {
    return path;
  }
  let resultPath = path;
  for (const { source, target } of moveHistory) {
    if (resultPath === source) {
      // the file itself was moved
      resultPath = target;
    } else if (resultPath.startsWith(source + '/')) {
      // a parent was moved
      resultPath = target + resultPath.substring(source.length);
    }
  }
  return resultPath;
};

const configurationReducer = createReducer(initialState)({
  [types.FETCH_VERSIONS_STARTED]: (state) => {
    return {
      ...incrementPendingRequests(state),
    };
  },
  [types.FETCH_VERSIONS_FAILURE]: (state) => {
    return {
      ...decrementPendingRequests(state),
    };
  },
  [types.FETCH_VERSIONS_SUCCESS]: (state, action) => {
    const { versions } = action.payload;
    return {
      ...decrementPendingRequests(state),
      versions,
    };
  },
  [types.FETCH_FILES_STARTED]: (state) => {
    return {
      ...incrementPendingRequests(state),
    };
  },
  [types.FETCH_FILES_FAILURE]: (state) => {
    return {
      ...decrementPendingRequests(state),
      files: [],
    };
  },
  [types.FETCH_FILES_SUCCESS]: (state, action) => {
    const { files } = action.payload;
    // remove the selection in case it does not exist anymore
    let selection = movePathIfRequired(state.selection, state.moveHistory);
    if (selection && !utils.getFile(files, selection)) {
      selection = null;
    }
    const unsavedFileContents = {};
    for (const path in state.unsavedFileContents) {
      const newPath = movePathIfRequired(path, state.moveHistory);
      if (utils.getFile(files, newPath)) {
        unsavedFileContents[newPath] = state.unsavedFileContents[path];
      }
    }
    return {
      ...decrementPendingRequests(state),
      files,
      moveHistory: [],
      selection,
      unsavedFileContents,
      updateDate: Date.now(),
    };
  },
  [types.SELECT_FILE]: (state, action) => {
    const { selection } = action.payload;
    return {
      ...state,
      selection,
      selectedDefaultConfigFile: null,
      selectedFileContent: null,
    };
  },
  [types.RESET]: () => {
    return {
      ...initialState,
    };
  },
  [types.FETCH_FILE_STARTED]: incrementPendingRequests,
  [types.FETCH_FILE_FAILURE]: decrementPendingRequests,
  [types.FETCH_FILE_SUCCESS]: (state, action) => {
    const { fileContent } = action.payload;
    return {
      ...decrementPendingRequests(state),
      selectedFileContent: fileContent,
    };
  },
  [types.DELETE_SELECTION_STARTED]: incrementPendingRequests,
  [types.DELETE_SELECTION_SUCCESS]: decrementPendingRequests,
  [types.DELETE_SELECTION_FAILURE]: decrementPendingRequests,
  [types.WRITE_FILE_STARTED]: incrementPendingRequests,
  [types.WRITE_FILE_SUCCESS]: (state, action) => {
    const { file, content } = action.payload;
    return {
      ...decrementPendingRequests(state),
      selectedFileContent: state.selection === file ? content : state.selectedFileContent,
      unsavedFileContents: omitBy(state.unsavedFileContents, (unsavedContent, path) => path === file && unsavedContent === content),
    };
  },
  [types.WRITE_FILE_FAILURE]: decrementPendingRequests,
  [types.CREATE_DIRECTORY_STARTED]: incrementPendingRequests,
  [types.CREATE_DIRECTORY_SUCCESS]: decrementPendingRequests,
  [types.CREATE_DIRECTORY_FAILURE]: decrementPendingRequests,
  [types.MOVE_STARTED]: incrementPendingRequests,
  [types.MOVE_SUCCESS]: (state, action) => {
    return {
      ...decrementPendingRequests(state),
      moveHistory: state.moveHistory.concat([action.payload]),
    };
  },
  [types.MOVE_FAILURE]: decrementPendingRequests,
  [types.SELECTED_FILE_CONTENTS_CHANGED]: (state, action) => {
    const { selection, selectedVersion, versions } = state;

    if (selection && (selectedVersion === null || selectedVersion === versions[0].id)) {
      const { selectedFileContent, unsavedFileContents } = state;
      const { content } = action.payload;
      const newUnsavedFileContents = { ...unsavedFileContents };

      if (content === null || selectedFileContent === content) {
        delete newUnsavedFileContents[selection];
      } else {
        newUnsavedFileContents[selection] = content;
      }
      return {
        ...state,
        unsavedFileContents: newUnsavedFileContents,
      };
    } else {
      return state;
    }
  },
  [types.SELECT_VERSION]: (state, action) => {
    const { version } = action.payload;
    return {
      ...state,
      selectedVersion: version,
    };
  },
  [types.FETCH_DEFAULT_CONFIG_STARTED]: incrementPendingRequests,
  [types.FETCH_DEFAULT_CONFIG_SUCCESS]: (state, action) => {
    const { defaultConfig } = action.payload;
    return {
      ...decrementPendingRequests(state),
      defaultConfig,
    };
  },
  [types.FETCH_DEFAULT_CONFIG_FAILURE]: decrementPendingRequests,
  [types.SELECT_DEFAULT_CONFIG_FILE]: (state, action) => {
    const { selection, content } = action.payload;
    return {
      ...state,
      selection: null,
      selectedDefaultConfigFile: selection,
      selectedFileContent: content,
    };
  },
  [types.FETCH_SCHEMA_STARTED]: incrementPendingRequests,
  [types.FETCH_SCHEMA_SUCCESS]: (state, action) => {
    const { schema } = action.payload;
    return {
      ...decrementPendingRequests(state),
      schema,
    };
  },
  [types.FETCH_SCHEMA_FAILURE]: decrementPendingRequests,

  [types.TOGGLE_VISUAL_CONFIGURATION_VIEW]: (state) => {
    return {
      ...state,
      showVisualConfigurationView: !state.showVisualConfigurationView,
    };
  },

  [types.TOGGLE_HISTORY_VIEW]: (state) => {
    return {
      ...state,
      currentSidebar: state.currentSidebar == SidebarTypes.HISTORY ? SidebarTypes.NONE : SidebarTypes.HISTORY,
    };
  },

  [types.TOGGLE_DOCUMENTATION_VIEW]: (state) => {
    return {
      ...state,
      currentSidebar: state.currentSidebar == SidebarTypes.CONFIGURATION_DOCS ? SidebarTypes.NONE : SidebarTypes.CONFIGURATION_DOCS,
    };
  },

  [types.TOGGLE_SHOW_HIDDEN_FILES]: (state) => {
    return {
      ...state,
      showHiddenFiles: !state.showHiddenFiles,
    };
  },
});

export default configurationReducer;
