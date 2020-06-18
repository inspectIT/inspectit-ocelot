import * as types from './types';
import { createReducer } from '../../utils';
import { promotion as initialState } from '../initial-states';
import _ from 'lodash';

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

    _.sortBy(diffEntries, ['file']);

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
    const { filename } = action.payload;
    return {
      ...state,
      currentSelection: filename,
    };
  },
  [types.APPROVE_FILE]: (state, action) => {
    const { file } = action.payload;

    const newFiles = _.cloneDeep(state.files);
    const targetFile = _.find(newFiles, { file });
    targetFile.approved = true;

    return {
      ...state,
      files: newFiles,
    };
  },
  [types.DISAPPROVE_FILE]: (state, action) => {
    const { file } = action.payload;

    const newFiles = _.cloneDeep(state.files);
    const targetFile = _.find(newFiles, { file });
    targetFile.approved = false;

    return {
      ...state,
      files: newFiles,
    };
  },

  [types.PROMOTE_CONFIGURATION_STARTED]: (state) => {
    return {
      ...state,
      pendingRequests: state.pendingRequests + 1,
    };
  },
  [types.PROMOTE_CONFIGURATION_SUCCESS]: (state) => {
    return {
      ...state,
      pendingRequests: state.pendingRequests - 1,
    };
  },
  [types.PROMOTE_CONFIGURATION_FAILURE]: (state) => {
    return {
      ...state,
      pendingRequests: state.pendingRequests - 1,
    };
  },
});

export default promotionReducer;
