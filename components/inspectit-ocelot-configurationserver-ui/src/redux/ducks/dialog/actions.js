import * as types from './types';

/**
 * Hides the currently shown dialog.
 */
export const hideDialogs = () => {
  return (dispatch) => {
    dispatch({ type: types.HIDE_DIALOG });
  };
};

/**
 * Shows the dialog with the given name. See '/components/dialogs/dialogs' for existing dialogs.
 *
 * @param {string} name
 * @param {object} payload
 */
export const showDialog = (name, payload = null) => {
  return (dispatch) => {
    dispatch({
      type: types.SHOW_DIALOG,
      payload: {
        dialog: name,
        payload,
      },
    });
  };
};
