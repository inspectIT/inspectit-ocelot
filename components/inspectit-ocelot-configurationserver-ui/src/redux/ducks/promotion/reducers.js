import * as types from './types';
import { createReducer } from '../../utils';
import { promotion as initialState } from '../initial-states';
import _ from 'lodash';

const promotionReducer = createReducer(initialState)({
  [types.APPROVE_FILE]: (state, action) => {
    const { filename } = action.payload;
    return {
      ...state,
      approvals: [...state.approvals, filename],
    };
  },

  [types.DISAPPROVE_FILE]: (state, action) => {
    const { filename } = action.payload;
    return {
      ...state,
      approvals: _.without(state.approvals, filename),
    };
  },

  [types.RESET_FILE_APPROVALS]: (state) => {
    return {
      ...state,
      approvals: [],
    };
  },

  [types.UPDATE_COMMIT_IDS]: (state, action) => {
    const { workspaceId, liveId } = action.payload;
    return {
      ...state,
      liveCommitId: liveId,
      workspaceCommitId: workspaceId,
    };
  },
});

export default promotionReducer;
