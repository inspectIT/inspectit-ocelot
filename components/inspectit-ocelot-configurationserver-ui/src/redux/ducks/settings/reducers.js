import * as types from './types';
import { createReducer } from '../../utils';
import { settings as initialState } from '../initial-states';

const settingsReducer = createReducer(initialState)({
  [types.FETCH_USERS_STARTED]: (state) => {
    return {
      ...state,
      pendingRequests: state.pendingRequests + 1,
    };
  },
  [types.FETCH_USERS_FAILURE]: (state) => {
    return {
      ...state,
      pendingRequests: state.pendingRequests - 1,
    };
  },
  [types.FETCH_USERS_SUCCESS]: (state, action) => {
    const { users } = action.payload;
    return {
      ...state,
      users,
      pendingRequests: state.pendingRequests - 1,
    };
  },
  [types.ADD_USER_STARTED]: (state) => {
    return {
      ...state,
      pendingRequests: state.pendingRequests + 1,
    };
  },
  [types.ADD_USER_FAILURE]: (state) => {
    return {
      ...state,
      pendingRequests: state.pendingRequests - 1,
    };
  },
  [types.ADD_USER_SUCCESS]: (state) => {
    return {
      ...state,
      pendingRequests: state.pendingRequests - 1,
    };
  },
  [types.DELETE_USER_STARTED]: (state) => {
    return {
      ...state,
      pendingRequests: state.pendingRequests + 1,
    };
  },
  [types.DELETE_USER_FAILURE]: (state) => {
    return {
      ...state,
      pendingRequests: state.pendingRequests - 1,
    };
  },
  [types.DELETE_USER_SUCCESS]: (state) => {
    return {
      ...state,
      pendingRequests: state.pendingRequests - 1,
    };
  },
  [types.CHANGE_PASSWORD_STARTED]: (state) => {
    return {
      ...state,
      pendingRequests: state.pendingRequests + 1,
    };
  },
  [types.CHANGE_PASSWORD_FAILURE]: (state) => {
    return {
      ...state,
      pendingRequests: state.pendingRequests - 1,
    };
  },
  [types.CHANGE_PASSWORD_SUCCESS]: (state) => {
    return {
      ...state,
      pendingRequests: state.pendingRequests - 1,
    };
  },
});

export default settingsReducer;
