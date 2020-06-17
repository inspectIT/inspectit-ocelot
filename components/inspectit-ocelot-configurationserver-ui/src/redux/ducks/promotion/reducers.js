import * as types from './types';
import { createReducer } from '../../utils';
import { promotion as initialState } from '../initial-states';

const promotionReducer = createReducer(initialState)({
  [types.FETCH_PROMOTIONS_STARTED]: (state) => {
    return {
      ...state,
      pendingRequests: state.pendingRequests + 1,
    };
  },
  [types.FETCH_PROMOTIONS_FAILURE]: (state) => {
    return {
      ...state,
      pendingRequests: state.pendingRequests - 1,
    };
  },
  [types.FETCH_PROMOTIONS_SUCCESS]: (state, action) => {
    const {
      promotions: { diffEntries, liveCommitId, workspaceCommitId },
    } = action.payload;

    const newState = {
      ...state,
      pendingRequests: state.pendingRequests - 1,
      updateDate: Date.now(),
      files: diffEntries,
      liveCommitId,
      workspaceCommitId,
    };

    if (liveCommitId !== state.liveCommitId || workspaceCommitId !== state.workspaceCommitId) {
      newState.currentSelection = initialState.currentSelection;
      newState.approvals = initialState.approvals;
    }

    return newState;
  },
  [types.SET_CURRENT_SELECTION]: (state, action) => {
    const { file } = action.payload;
    return {
      ...state,
      currentSelection: file,
    };
  },
  [types.APPROVE_FILE]: (state, action) => {
    const { file } = action.payload;

    return {
      ...state,
      approvals: [...state.approvals, file]
    };
  },
  [types.DISAPPROVE_FILE]: (state, action) => {
    const { file } = action.payload;
    
    return {
      ...state,
      approvals: state.approvals.filter(e => e !== file).map(e => e)
    };
  },
});

export default promotionReducer;
