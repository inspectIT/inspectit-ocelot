import * as types from "./types";
import { createReducer } from "../../utils";

const initialState = {
    lastNotification: null
};

const notificationReducer = createReducer(initialState)({
    [types.SHOW]: (state, action) => {
        const { payload } = action;
        const notification = {
            ...payload
        };
        return {
            ...state,
            lastNotification: notification
        };
    }
});

export default notificationReducer;
