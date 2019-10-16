import * as types from "./types";
import { createReducer } from "../../utils";
import { mappings as initialState } from '../initial-states';
import { cloneDeep } from 'lodash'

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
        const { pendingRequests } = state;
        const { mappings } = action.payload;
        return {
            ...state,
            pendingRequests: pendingRequests - 1,
            mappings,
            editableMappings: cloneDeep(mappings),
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
        const { mappings } = action.payload;
        return {
            ...state,
            pendingRequests: state.pendingRequests - 1,
            mappings,
            editableMappings: cloneDeep(mappings),
            updateDate: Date.now()
        };
    },
    [types.DELETE_MAPPING_STARTED]: (state, action) => {
        return {
            ...state,
            pendingRequests: state.pendingRequests + 1
        }
    },
    [types.DELETE_MAPPING_FAILURE]: (state, action) => {
        return {
            ...state,
            pendingRequests: state.pendingRequests - 1
        }
    },
    [types.DELETE_MAPPING_SUCCESS]: (state, action) => {
        return {
            ...state,
            pendingRequests: state.pendingRequests - 1
        }
    }
});

export default mappingsReducer;
