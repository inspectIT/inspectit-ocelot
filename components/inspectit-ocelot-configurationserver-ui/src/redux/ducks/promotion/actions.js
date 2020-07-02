import * as types from './types';

/**
 * Approve the given file and marks it ready for promotion.
 *
 * @param {string} filename
 */
export const approveFile = (filename) => {
  return (dispatch) => {
    dispatch({ type: types.APPROVE_FILE, payload: { filename } });
  };
};

/**
 * Disapprove the given file, so it will not be promoted.
 *
 * @param {string} filename
 */
export const disapproveFile = (filename) => {
  return (dispatch) => {
    dispatch({ type: types.DISAPPROVE_FILE, payload: { filename } });
  };
};

/**
 * Resets the currently approved files.
 */
export const resetFileApprovals = () => {
  return (dispatch) => {
    dispatch({ type: types.RESET_FILE_APPROVALS });
  };
};

/**
 * Updates the state's commit ids with the given ones. In case the existing ids differs from the
 * specified ones, the file approvals will be resetted.
 *
 * @param {string} workspaceId
 * @param {string} liveId
 */
export const updateCommitIds = (workspaceId, liveId) => {
  return (dispatch, getState) => {
    const { workspaceCommitId, liveCommitId } = getState().promotion;

    dispatch({ type: types.UPDATE_COMMIT_IDS, payload: { workspaceId, liveId } });

    if (workspaceCommitId !== workspaceId || liveCommitId !== liveId) {
      dispatch(resetFileApprovals());
    }
  };
};
