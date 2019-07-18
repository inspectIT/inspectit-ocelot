import * as types from "./types";
import { createReducer } from "../../utils";

const initialState = {
    notifications: []
};

let notificationCount = 0;

const notificationReducer = createReducer(initialState)({
    [types.SHOW]: (state, action) => {
        const { payload } = action;
        const notifications = [{
            ...payload,
            id: notificationCount++
        }];
        return {
            ...state,
            notifications
        };
    }
});

export default notificationReducer;
