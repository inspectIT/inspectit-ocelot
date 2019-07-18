import * as types from "./types";
import { createReducer } from "../../utils";

const initialState = {
    loading: false,
    error: null,
    files: [],
    updateDate: null,
    selection: null
};

const configurationReducer = createReducer(initialState)({
    [types.FETCH_FILES_STARTED]: (state, action) => {
        return {
            ...state,
            loading: true
        };
    },
    [types.FETCH_FILES_FAILURE]: (state, action) => {
        const { error } = action.payload;
        return {
            ...state,
            loading: false,
            error: error,
            files: []
        };
    },
    [types.FETCH_FILES_SUCCESS]: (state, action) => {
        const { fileRoot, files } = action.payload;
        return {
            ...state,
            loading: false,
            error: null,
            files,
            updateDate: Date.now()
        };
    },
    [types.SELECT_FILE]: (state, action) => {
        const { selection } = action.payload;
        return {
            ...state,
            selection
        };
    }
});

export default configurationReducer;
