import * as types from "./types";
import { createReducer } from "../../utils";

const initialState = {
    token: null,
    loading: false,
    error: null
};

const authorizationReducer = createReducer(initialState)({
    [types.FETCH_TOKEN_STARTED]: (state, action) => {
        return {
            ...state,
            loading: true
        };
    },
    [types.FETCH_TOKEN_FAILURE]: (state, action) => {
        const {error} = action.payload;
        return {
            ...state,
            loading: false,
            error: error,
            token: null
        };
    },
    [types.FETCH_TOKEN_SUCCESS]: (state, action) => {
        const {token} = action.payload;
        return {
            ...state,
            loading: false,
            error: null,
            token
        };
    }
});

export default authorizationReducer;
