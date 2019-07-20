import * as types from "./types";
import { createReducer } from "../../utils";
import { configuration as initialState } from '../initial-states';

const configurationReducer = createReducer(initialState)({
    [types.FETCH_FILES_STARTED]: (state, action) => {
        return {
            ...state,
            loading: true
        };
    },
    [types.FETCH_FILES_FAILURE]: (state, action) => {
        return {
            ...state,
            loading: false,
            files: []
        };
    },
    [types.FETCH_FILES_SUCCESS]: (state, action) => {
        const { fileRoot, files } = action.payload;
        return {
            ...state,
            loading: false,
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
    },
    [types.RESET]: (state, action) => {
        return {
            ...initialState
        };
    }
});

export default configurationReducer;
