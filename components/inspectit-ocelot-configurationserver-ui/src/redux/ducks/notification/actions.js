import * as types from './types';

let notificationCount = 0;

/**
 * Shows a success notification message.
 *
 * @param {string} summary
 * @param {string} detail
 */
export const showSuccessMessage = (summary, detail) => ({
  type: types.SHOW,
  payload: {
    severity: 'success',
    summary,
    detail,
    id: notificationCount++,
  },
});

/**
 * Shows an info notification message.
 *
 * @param {string} summary
 * @param {string} detail
 */
export const showInfoMessage = (summary, detail) => ({
  type: types.SHOW,
  payload: {
    severity: 'info',
    summary,
    detail,
    id: notificationCount++,
  },
});

/**
 * Shows a warning notification message.
 *
 * @param {string} summary
 * @param {string} detail
 */
export const showWarningMessage = (summary, detail) => ({
  type: types.SHOW,
  payload: {
    severity: 'warn',
    summary,
    detail,
    id: notificationCount++,
  },
});

/**
 * Shows an error notification message.
 *
 * @param {string} summary
 * @param {string} detail
 */
export const showErrorMessage = (summary, detail) => ({
  type: types.SHOW,
  payload: {
    severity: 'error',
    summary,
    detail,
    id: notificationCount++,
  },
});
