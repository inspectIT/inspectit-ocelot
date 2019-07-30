import * as types from "./types";
import { createReducer } from "../../utils";
import { mappings as initialState } from '../initial-states';
import * as selectors from "./selectors";
import yaml from 'js-yaml';

const mappingsReducer = createReducer(initialState)({
    [types.FETCH_MAPPINGS_STARTED]: (state, action) => {
        return {
            ...state,
            pendingRequests: state.pendingRequests + 1
        };
    },
    [types.FETCH_MAPPINGS_FAILURE]: (state, action) => {
        return {
            ...state,
            pendingRequests: state.pendingRequests - 1
        };
    },
    [types.FETCH_MAPPINGS_SUCCESS]: (state, action) => {
        const { mappings } = action.payload;
        const mappingsYaml = mappings ? yaml.safeDump(mappings) : "";
        return {
            ...state,
            pendingRequests: state.pendingRequests - 1,
            mappings,
            editorContent: state.editorContent == mappingsYaml ? null : state.editorContent,
            updateDate: Date.now()
        };
    },
    [types.PUT_MAPPINGS_STARTED]: (state, action) => {
        return {
            ...state,
            pendingRequests: state.pendingRequests + 1
        };
    },
    [types.PUT_MAPPINGS_FAILURE]: (state, action) => {
        return {
            ...state,
            pendingRequests: state.pendingRequests - 1
        };
    },
    [types.PUT_MAPPINGS_SUCCESS]: (state, action) => {
        const { mappings, resetEditor } = action.payload;
        return {
            ...state,
            pendingRequests: state.pendingRequests - 1,
            editorContent: resetEditor ? null : state.editorContent,
            mappings,
            updateDate: Date.now()
        };
    },
    [types.EDITOR_CONTENT_CHANGED]: (state, action) => {
        const { content } = action.payload;

        return {
            ...state,
            editorContent: selectors.getMappingsAsYamlFromMappingsState(state) == content ? null : content
        };
    }
});

export default mappingsReducer;
