import * as types from "./types";
import * as queries from "./queries";
import { createReducer } from "../../utils";
import { configuration as initialState } from '../initial-states';


function incrementLoadingCount(state) {
    return {
        ...state,
        loading: state.loading + 1
    };
}

function decrementLoadingCount(state) {
    return {
        ...state,
        loading: state.loading - 1
    };
}

const configurationReducer = createReducer(initialState)({
    [types.FETCH_FILES_STARTED]: (state, action) => {
        return {
            ...state,
            loading: state.loading + 1
        };
    },
    [types.FETCH_FILES_FAILURE]: (state, action) => {
        return {
            ...state,
            loading: state.loading - 1,
            files: []
        };
    },
    [types.FETCH_FILES_SUCCESS]: (state, action) => {
        const { files } = action.payload;
        //remove the selection in case it does not exist anymore
        let selection = state.selection;
        if(selection && !queries.getFile(files, selection)) {
            selection = null;
        }
        return {
            ...state,
            loading: state.loading - 1,
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
    [types.DELETE_SELECTION_STARTED]: incrementLoadingCount,
    [types.DELETE_SELECTION_SUCCESS]: decrementLoadingCount,
    [types.DELETE_SELECTION_FAILURE]: decrementLoadingCount,
    [types.WRITE_FILE_STARTED]: incrementLoadingCount,
    [types.WRITE_FILE_SUCCESS]: decrementLoadingCount,
    [types.WRITE_FILE_FAILURE]: decrementLoadingCount,
    [types.CREATE_DIRECTORY_STARTED]: incrementLoadingCount,
    [types.CREATE_DIRECTORY_SUCCESS]: decrementLoadingCount,
    [types.CREATE_DIRECTORY_FAILURE]: decrementLoadingCount

});

export default configurationReducer;
