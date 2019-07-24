import * as types from "./types";
import * as utils from "./utils";
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
        let selection = state.selection;
        if (selection && !utils.getFile(files, selection)) {
            selection = null;
        }
        return {
            ...state,
            pendingRequests: state.pendingRequests - 1,
            files,
            selection,
            updateDate: Date.now()
        };
    },
    [types.SELECT_FILE]: (state, action) => {
        const { selection } = action.payload;
        return {
            ...state,
            selection
        };
    },
    [types.RESET]: (state, action) => {
        return {
            ...initialState
        };
    },
    [types.DELETE_SELECTION_STARTED]: incrementPendingRequests,
    [types.DELETE_SELECTION_SUCCESS]: decrementPendingRequests,
    [types.DELETE_SELECTION_FAILURE]: decrementPendingRequests,
    [types.WRITE_FILE_STARTED]: incrementPendingRequests,
    [types.WRITE_FILE_SUCCESS]: decrementPendingRequests,
    [types.WRITE_FILE_FAILURE]: decrementPendingRequests,
    [types.CREATE_DIRECTORY_STARTED]: incrementPendingRequests,
    [types.CREATE_DIRECTORY_SUCCESS]: decrementPendingRequests,
    [types.CREATE_DIRECTORY_FAILURE]: decrementPendingRequests

});

export default configurationReducer;
