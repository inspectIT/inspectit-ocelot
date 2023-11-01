import * as types from './types';
import { createReducer } from '../../utils';
import { mappings as initialState } from '../initial-states';

const mappingsReducer = createReducer(initialState)({
  [types.FETCH_MAPPINGS_STARTED]: (state) => {
    return {
      ...state,
      pendingRequests: state.pendingRequests + 1,
    };
  },
  [types.FETCH_MAPPINGS_FAILURE]: (state) => {
    return {
      ...state,
      pendingRequests: state.pendingRequests - 1,
    };
  },
  [types.FETCH_MAPPINGS_SUCCESS]: (state, action) => {
    const { mappings } = action.payload;
    return {
      ...state,
      pendingRequests: state.pendingRequests - 1,
      mappings,
      updateDate: Date.now(),
    };
  },
  [types.PUT_MAPPINGS_STARTED]: (state) => {
    return {
      ...state,
      pendingRequests: state.pendingRequests + 1,
    };
  },
  [types.PUT_MAPPINGS_FAILURE]: (state) => {
    return {
      ...state,
      pendingRequests: state.pendingRequests - 1,
    };
  },
  [types.PUT_MAPPINGS_SUCCESS]: (state) => {
    return {
      ...state,
      pendingRequests: state.pendingRequests - 1,
      updateDate: Date.now(),
    };
  },
  [types.PUT_MAPPING_STARTED]: (state) => {
    return {
      ...state,
      pendingRequests: state.pendingRequests + 1,
    };
  },
  [types.PUT_MAPPING_FAILURE]: (state) => {
    return {
      ...state,
      pendingRequests: state.pendingRequests - 1,
    };
  },
  [types.PUT_MAPPING_SUCCESS]: (state) => {
    return {
      ...state,
      pendingRequests: state.pendingRequests - 1,
      updateDate: Date.now(),
    };
  },
  [types.DELETE_MAPPING_STARTED]: (state) => {
    return {
      ...state,
      pendingRequests: state.pendingRequests + 1,
    };
  },
  [types.DELETE_MAPPING_FAILURE]: (state) => {
    return {
      ...state,
      pendingRequests: state.pendingRequests - 1,
    };
  },
  [types.DELETE_MAPPING_SUCCESS]: (state) => {
    return {
      ...state,
      pendingRequests: state.pendingRequests - 1,
    };
  },
  [types.FETCH_MAPPINGS_SOURCE_BRANCH_STARTED]: (state) => {
    return {
      ...state,
      pendingRequests: state.pendingRequests + 1,
    };
  },
  [types.FETCH_MAPPINGS_SOURCE_BRANCH_SUCCESS]: (state, action) => {
    const sourceBranch = action.payload.sourceBranch;
    return {
      ...state,
      sourceBranch,
      pendingRequests: state.pendingRequests - 1,
    };
  },
  [types.FETCH_MAPPINGS_SOURCE_BRANCH_FAILURE]: (state) => {
    return {
      ...state,
      pendingRequests: state.pendingRequests - 1,
    };
  },
  [types.PUT_MAPPINGS_SOURCE_BRANCH_STARTED]: (state) => {
    return {
      ...state,
      pendingRequests: state.pendingRequests + 1,
    };
  },
  [types.PUT_MAPPINGS_SOURCE_BRANCH_SUCCESS]: (state, action) => {
    const sourceBranch = action.payload.sourceBranch;
    return {
      ...state,
      sourceBranch,
      pendingRequests: state.pendingRequests - 1,
    };
  },
  [types.PUT_MAPPINGS_SOURCE_BRANCH_FAILURE]: (state) => {
    return {
      ...state,
      pendingRequests: state.pendingRequests - 1,
    };
  },
});

export default mappingsReducer;
