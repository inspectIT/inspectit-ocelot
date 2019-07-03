import * as types from "./types";
import { createReducer } from "../../utils";

const initialState = {
    lastUpdate: 0,
    light: false,
    count: 0
};

const clockReducer = createReducer(initialState)({
    [types.TICK]: (state, action) => {
        return {
            ...state,
            lastUpdate: action.ts,
            light: !!action.light
        };
    },
    [types.INCREMENT]: (state, action) => {
        return {
            ...state,
            count: state.count + 1
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
