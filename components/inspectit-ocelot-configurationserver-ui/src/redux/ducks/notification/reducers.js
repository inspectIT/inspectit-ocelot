import * as types from './types';
import { createReducer } from '../../utils';
import { notification as initialState } from '../initial-states';

const notificationReducer = createReducer(initialState)({
  [types.SHOW]: (state, action) => {
    const { payload } = action;
    const notification = {
      ...payload,
    };
    return {
      ...state,
      lastNotification: notification,
    };
  },
});

export default notificationReducer;
