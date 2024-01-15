import * as types from './types';
import { createReducer } from '../../utils';
import { mappings as initialState } from '../initial-states';
import SidebarTypes from '../../../components/views/mappings/SidebarTypes';

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

const mappingsReducer = createReducer(initialState)({
  [types.FETCH_MAPPINGS_STARTED]: (state) => {
    return {
      ...incrementPendingRequests(state),
      isLoading: true,
    };
  },
  [types.FETCH_MAPPINGS_FAILURE]: (state) => {
    return {
      ...decrementPendingRequests(state),
      isLoading: false,
    };
  },
  [types.FETCH_MAPPINGS_SUCCESS]: (state, action) => {
    const { mappings } = action.payload;
    return {
      ...decrementPendingRequests(state),
      mappings,
      updateDate: Date.now(),
      isLoading: false,
    };
  },
  [types.PUT_MAPPINGS_STARTED]: (state) => {
    return {
      ...incrementPendingRequests(state),
    };
  },
  [types.PUT_MAPPINGS_FAILURE]: (state) => {
    return {
      ...decrementPendingRequests(state),
    };
  },
  [types.PUT_MAPPINGS_SUCCESS]: (state) => {
    return {
      ...decrementPendingRequests(state),
      updateDate: Date.now(),
    };
  },
  [types.PUT_MAPPING_STARTED]: (state) => {
    return {
      ...incrementPendingRequests(state),
    };
  },
  [types.PUT_MAPPING_FAILURE]: (state) => {
    return {
      ...decrementPendingRequests(state),
    };
  },
  [types.PUT_MAPPING_SUCCESS]: (state) => {
    return {
      ...decrementPendingRequests(state),
      updateDate: Date.now(),
    };
  },
  [types.DELETE_MAPPING_STARTED]: (state) => {
    return {
      ...incrementPendingRequests(state),
    };
  },
  [types.DELETE_MAPPING_FAILURE]: (state) => {
    return {
      ...decrementPendingRequests(state),
    };
  },
  [types.DELETE_MAPPING_SUCCESS]: (state) => {
    return {
      ...decrementPendingRequests(state),
    };
  },
  [types.FETCH_MAPPINGS_SOURCE_BRANCH_STARTED]: (state) => {
    return {
      ...incrementPendingRequests(state),
    };
  },
  [types.FETCH_MAPPINGS_SOURCE_BRANCH_SUCCESS]: (state, action) => {
    const sourceBranch = action.payload.sourceBranch;
    return {
      ...decrementPendingRequests(state),
      sourceBranch,
    };
  },
  [types.FETCH_MAPPINGS_SOURCE_BRANCH_FAILURE]: (state) => {
    return {
      ...decrementPendingRequests(state),
    };
  },
  [types.PUT_MAPPINGS_SOURCE_BRANCH_STARTED]: (state) => {
    return {
      ...incrementPendingRequests(state),
    };
  },
  [types.PUT_MAPPINGS_SOURCE_BRANCH_SUCCESS]: (state, action) => {
    const sourceBranch = action.payload.sourceBranch;
    return {
      ...decrementPendingRequests(state),
      sourceBranch,
    };
  },
  [types.PUT_MAPPINGS_SOURCE_BRANCH_FAILURE]: (state) => {
    return {
      ...decrementPendingRequests(state),
    };
  },
  [types.FETCH_VERSIONS_STARTED]: (state) => {
    return {
      ...incrementPendingRequests(state),
    };
  },
  [types.FETCH_VERSIONS_FAILURE]: (state) => {
    return {
      ...decrementPendingRequests(state),
    };
  },
  [types.FETCH_VERSIONS_SUCCESS]: (state, action) => {
    const { versions } = action.payload;
    return {
      ...decrementPendingRequests(state),
      versions,
    };
  },
  [types.SELECT_VERSION]: (state, action) => {
    const { version } = action.payload;
    return {
      ...state,
      selectedVersion: version,
    };
  },
  [types.TOGGLE_HISTORY_VIEW]: (state) => {
    return {
      ...state,
      currentSidebar: state.currentSidebar == SidebarTypes.HISTORY ? SidebarTypes.NONE : SidebarTypes.HISTORY,
    };
  },
});

export default mappingsReducer;
