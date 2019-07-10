import * as types from "./types";
import { createReducer } from "../../utils";

const initialState = {
    lastUpdate: 0,
    light: false,
    count: 0
};

const clockReducer = createReducer(initialState)({
    [types.INIT]: (state, action) => {
        const { ts, isServer } = action.payload;
        return {
            ...state,
            lastUpdate: ts,
            light: !isServer,
            serverRendered: isServer
        };
    },
    [types.TICK]: (state, action) => {
        const { ts, light } = action.payload;
        return {
            ...state,
            lastUpdate: ts,
            light: !!light
        };
    },
    [types.INCREMENT]: (state, action) => {
        const { value } = action.payload;
        return {
            ...state,
            count: state.count + value
        };
    },
    [types.DECREMENT]: (state, action) => {
        return {
            ...state,
            count: state.count - 1
        };
    },
    [types.RESET]: (state, action) => {
        return {
            ...state,
            count: initialState.count
        };
    }
});

export default clockReducer;
