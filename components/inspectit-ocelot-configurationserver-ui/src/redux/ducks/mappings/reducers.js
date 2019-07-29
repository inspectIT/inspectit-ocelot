import * as types from "./types";
import { createReducer } from "../../utils";
import { mappings as initialState } from '../initial-states';

const authorizationReducer = createReducer(initialState)({
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
        return {
            ...state,
            pendingRequests: state.pendingRequests - 1,
            mappings,
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
            updateDate: Date.now()
        };
    }
});

export default authorizationReducer;
