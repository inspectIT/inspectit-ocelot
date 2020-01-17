import * as types from "./types";
import { createReducer } from "../../utils";
import { settings as initialState } from '../initial-states';

const settingsReducer = createReducer(initialState)({
    [types.FETCH_USERS_STARTED]: (state, action) => {
        return {
            ...state,
            pendingRequests: state.pendingRequests + 1
        };
    },
    [types.FETCH_USERS_FAILURE]: (state, action) => {
        return {
            ...state,
            pendingRequests: state.pendingRequests - 1
        };
    },
    [types.FETCH_USERS_SUCCESS]: (state, action) => {
        const { users } = action.payload;
        return {
            ...state,
            users,
            pendingRequests: state.pendingRequests - 1

        };
    },
    [types.ADD_USER_STARTED]: (state, action) => {
        return {
            ...state,
            pendingRequests: state.pendingRequests + 1
        };
    },
    [types.ADD_USER_FAILURE]: (state, action) => {
        return {
            ...state,
            pendingRequests: state.pendingRequests - 1
        };
    },
    [types.ADD_USER_SUCCESS]: (state, action) => {
        return {
            ...state,
            pendingRequests: state.pendingRequests - 1
        };
    },
})

export default settingsReducer; 