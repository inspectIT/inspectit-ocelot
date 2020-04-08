import * as types from './types';
import { createReducer } from '../../utils';
import { agentStatus as initialState } from '../initial-states';

const incrementPendingRequests = (state) => {
  return {
    ...state,
    pendingRequests: state.pendingRequests + 1,
  };
};

const decrementPendingRequests = (state) => {
  return {
    ...state,
    pendingRequests: state.pendingRequests - 1,
  };
};

const agentStatusReducer = createReducer(initialState)({
  [types.FETCH_STATUS_STARTED]: incrementPendingRequests,
  [types.FETCH_STATUS_SUCCESS]: (state, action) => {
    const {
      payload: { agents },
    } = action;
    return {
      ...state,
      agents,
      pendingRequests: state.pendingRequests - 1,
    };
  },
  [types.FETCH_STATUS_FAILURE]: decrementPendingRequests,
  [types.CLEAR_STATUS_STARTED]: (state) => {
    return {
      ...state,
      pendingRequests: state.pendingRequests + 1,
      pendingClearRequests: state.pendingClearRequests + 1,
    };
  },
  [types.CLEAR_STATUS_SUCCESS]: (state) => {
    return {
      ...state,
      pendingRequests: state.pendingRequests - 1,
      pendingClearRequests: state.pendingClearRequests - 1,
    };
  },
  [types.CLEAR_STATUS_FAILURE]: (state) => {
    return {
      ...state,
      pendingRequests: state.pendingRequests - 1,
      pendingClearRequests: state.pendingClearRequests - 1,
    };
  },
});

export default agentStatusReducer;
