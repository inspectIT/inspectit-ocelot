import * as types from "./types";
import * as utils from "./utils";
import { mapKeys, omitBy } from "lodash"
import { createReducer } from "../../utils";
import { configuration as initialState } from '../initial-states';


const incrementPendingRequests = (state) => {
    return {
        ...state,
        pendingRequests: state.pendingRequests + 1
    };
}

const decrementPendingRequests = (state) => {
    return {
        ...state,
        pendingRequests: state.pendingRequests - 1
    };
}

const movePathIfRequired = (path, moveHistory) => {
    if (!path) return path;
    let resultPath = path;
    for (const { source, target } of moveHistory) {
        if (resultPath == source) { //the file itself was moved
            resultPath = target;
        } else if (resultPath.startsWith(source + "/")) { //a parent was moved
            resultPath = target + resultPath.substring(source.length);
        }
    }
    return resultPath;
}

const configurationReducer = createReducer(initialState)({
    [types.FETCH_FILES_STARTED]: (state, action) => {
        return {
            ...state,
            pendingRequests: state.pendingRequests + 1
        };
    },
    [types.FETCH_FILES_FAILURE]: (state, action) => {
        return {
            ...state,
            pendingRequests: state.pendingRequests - 1,
            files: []
        };
    },
    [types.FETCH_FILES_SUCCESS]: (state, action) => {
        const { files } = action.payload;
        //remove the selection in case it does not exist anymore
        let selection = movePathIfRequired(state.selection, state.moveHistory);
        if (selection && !utils.getFile(files, selection)) {
            selection = null;
        }
        let unsavedFileContents = {};
        for (const path in state.unsavedFileContents) {
            let newPath = movePathIfRequired(path, state.moveHistory);
            if (utils.getFile(files, newPath)) {
                unsavedFileContents[newPath] = state.unsavedFileContents[path];
            }
        }
        return {
            ...state,
            pendingRequests: state.pendingRequests - 1,
            files,
            moveHistory: [],
            selection,
            unsavedFileContents,
            updateDate: Date.now()
        };
    },
    [types.SELECT_FILE]: (state, action) => {
        const { selection } = action.payload;
        return {
            ...state,
            selection,
            selectedFileContent: null
        };
    },
    [types.RESET]: (state, action) => {
        return {
            ...initialState
        };
    },
    [types.FETCH_FILE_STARTED]: (state, action) => {
        return {
            ...state,
            pendingRequests: state.pendingRequests + 1
        };
    },
    [types.FETCH_FILE_FAILURE]: (state, action) => {
        return {
            ...state,
            pendingRequests: state.pendingRequests - 1
        };
    },
    [types.FETCH_FILE_SUCCESS]: (state, action) => {
        const { fileContent } = action.payload;
        return {
            ...state,
            pendingRequests: state.pendingRequests - 1,
            selectedFileContent: fileContent
        };
    },
    [types.DELETE_SELECTION_STARTED]: incrementPendingRequests,
    [types.DELETE_SELECTION_SUCCESS]: decrementPendingRequests,
    [types.DELETE_SELECTION_FAILURE]: decrementPendingRequests,
    [types.WRITE_FILE_STARTED]: incrementPendingRequests,
    [types.WRITE_FILE_SUCCESS]: (state, action) => {
        const { file, content } = action.payload;
        return {
            ...state,
            pendingRequests: state.pendingRequests - 1,
            selectedFileContent: state.selection == file ? content : state.selectedFileContent,
            unsavedFileContents: omitBy(state.unsavedFileContents, (unsavedContent, path) => path == file && unsavedContent == content)
        };
    },
    [types.WRITE_FILE_FAILURE]: decrementPendingRequests,
    [types.CREATE_DIRECTORY_STARTED]: incrementPendingRequests,
    [types.CREATE_DIRECTORY_SUCCESS]: decrementPendingRequests,
    [types.CREATE_DIRECTORY_FAILURE]: decrementPendingRequests,
    [types.MOVE_STARTED]: incrementPendingRequests,
    [types.MOVE_SUCCESS]: (state, action) => {
        return {
            ...state,
            pendingRequests: state.pendingRequests - 1,
            moveHistory: state.moveHistory.concat([action.payload])
        };
    },
    [types.MOVE_FAILURE]: decrementPendingRequests,
    [types.SELECTED_FILE_CONTENTS_CHANGED]: (state, action) => {
        const { selection } = state;
        if (selection) {
            const { selectedFileContent, unsavedFileContents } = state;
            const { content } = action.payload;
            const newUnsavedFileContents = { ...unsavedFileContents };

            if (content == null || selectedFileContent == content) {
                delete newUnsavedFileContents[selection];
            } else {
                newUnsavedFileContents[selection] = content;
            }

            return {
                ...state,
                unsavedFileContents: newUnsavedFileContents
            }
        } else {
            return state;
        }
    }

});

export default configurationReducer;
