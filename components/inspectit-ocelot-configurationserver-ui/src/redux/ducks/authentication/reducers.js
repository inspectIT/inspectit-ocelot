import * as types from "./types";
import { createReducer } from "../../utils";
import { isTokenExpired } from '../../../lib/jwt-utils';
import { authentication as initialState } from '../initial-states';

const authorizationReducer = createReducer(initialState)({
    [types.FETCH_TOKEN_STARTED]: (state, action) => {
        return {
            ...state,
            loading: true,
        };
    },
    [types.FETCH_TOKEN_FAILURE]: (state, action) => {
        const { error } = action.payload;
        return {
            ...state,
            loading: false,
            error: error,
            token: null
        };
    },
    [types.FETCH_TOKEN_SUCCESS]: (state, action) => {
        const { token, username } = action.payload;
        return {
            ...state,
            loading: false,
            error: null,
            token,
            username: username.toLowerCase()
        };
    },
    [types.LOGOUT]: (state, action) => {
        return {
            ...state,
            loading: false,
            error: null,
            token: null,
            username: null
        };
    },
    [types.RENEW_TOKEN_SUCCESS]: (state, action) => {
        const { token } = action.payload;
        return {
            ...state,
            error: null,
            token
        };
    },
    // SPECIAL REDUCER - dispatched by redux-persist to rehydrate store
    ["persist/REHYDRATE"]: (state, action) => {
        if (!action.payload || !action.payload.authentication) {
            return { ...state };
        }

        const { token, username } = action.payload.authentication;
        if (token) {
            const expired = isTokenExpired(token);
            if (!expired) {
                return {
                    ...initialState,
                    token,
                    username
                };
            }
        }

        return initialState;
    }
});

export default authorizationReducer;
