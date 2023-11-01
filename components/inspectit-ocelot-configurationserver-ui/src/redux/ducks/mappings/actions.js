import * as types from './types';
import axios from '../../../lib/axios-api';
import { notificationActions } from '../notification';
import { VERSION_LIMIT } from '../../../data/constants';

/**
 * Fetches the agent mappings from the server.
 */
export const fetchMappings = () => {
  return (dispatch, getState) => {
    const { selectedVersion } = getState().mappings;

    const params = {};
    if (selectedVersion) {
      params.version = selectedVersion;
    }
    dispatch({ type: types.FETCH_MAPPINGS_STARTED });
    axios
      .get('/mappings', { params })
      .then((response) => {
        const mappings = response.data;
        dispatch({ type: types.FETCH_MAPPINGS_SUCCESS, payload: { mappings } });
      })
      .catch(() => {
        dispatch({ type: types.FETCH_MAPPINGS_FAILURE });
      });
  };
};

/**
 * Stores the given agent mappings on the server. This mapping replaces the current agent mappings.
 * The given mappings will also be stored in the state when the request was successful.
 *
 * @param {*} mappings - the agent mappings to store
 */
export const putMappings = (mappings, onComplete = () => {}) => {
  return (dispatch) => {
    dispatch({ type: types.PUT_MAPPINGS_STARTED });

    axios
      .put('/mappings', mappings, {
        headers: { 'content-type': 'application/json' },
      })
      .then(() => {
        dispatch({ type: types.PUT_MAPPINGS_SUCCESS, payload: { mappings } });
        dispatch(notificationActions.showSuccessMessage('Agent Mappings Saved', 'The agent mappings have been successfully saved.'));
        dispatch(fetchMappings());
        onComplete(true);
      })
      .catch(() => {
        dispatch({ type: types.PUT_MAPPINGS_FAILURE });
        onComplete(false);
      });
  };
};

/**
 * Stores the given agent mapping on the server.
 * This mapping replaces other mappings with the same name or creates a new mapping
 * and triggers fetchMappings in case of success
 *
 * @param {*} mappings - the agent mapping to store
 */
export const putMapping = (mapping, onComplete = () => {}) => {
  return (dispatch) => {
    dispatch({ type: types.PUT_MAPPING_STARTED });

    axios
      .put(`/mappings/${mapping.name}`, mapping, {
        headers: { 'content-type': 'application/json' },
      })
      .then(() => {
        dispatch({ type: types.PUT_MAPPING_SUCCESS });
        dispatch(notificationActions.showSuccessMessage('Agent Mappings Saved', 'The agent mappings have been successfully saved.'));
        onComplete(true);

        dispatch(fetchMappings());
      })
      .catch(() => {
        dispatch({ type: types.PUT_MAPPING_FAILURE });
        onComplete(false);
      });
  };
};

/**
 * Deletes the given mapping respectively its mapping name
 * and triggers fetchMappings in case of success
 *
 * @param {mapping || string} mapping - the mapping or mapping name which should be deleted
 */
export const deleteMapping = (mapping) => {
  const nameToDelete = mapping.name ? mapping.name : mapping;

  return (dispatch) => {
    dispatch({ type: types.DELETE_MAPPING_STARTED });

    axios
      .delete(`/mappings/${nameToDelete}`)
      .then(() => {
        dispatch({ type: types.DELETE_MAPPING_SUCCESS });
        dispatch(notificationActions.showSuccessMessage('Mapping Deleted', 'The mapping has been successfully deleted.'));
        dispatch(fetchMappings());
      })
      .catch(() => {
        dispatch({ type: types.DELETE_MAPPING_FAILURE });
      });
  };
};

/**
 * Fetches all existing versions.
 */
export const fetchMappingsVersions = () => {
  return (dispatch) => {
    dispatch({ type: types.FETCH_VERSIONS_STARTED });

    const params = {
      limit: VERSION_LIMIT,
    };

    axios('/versions', { params })
      .then((res) => {
        const versions = res.data;
        dispatch({ type: types.FETCH_VERSIONS_SUCCESS, payload: { versions } });
      })
      .catch(() => {
        dispatch({ type: types.FETCH_VERSIONS_FAILURE });
      });
  };
};

/**
 * Selects the version with the given id.
 */
export const selectMappingsVersion = (version, reloadMappings = true) => {
  return (dispatch) => {
    // changing the selected version
    dispatch({
      type: types.SELECT_VERSION,
      payload: {
        version,
      },
    });

    if (reloadMappings) {
      // fetching the content of the selected version
      dispatch(fetchMappings());
    }
  };
};

/**
 * Send the new source branch for the agent mappings file itself
 * @param branch new source branch
 */
export const putMappingsSourceBranch = (branch, onComplete = () => {}) => {
  return (dispatch) => {
    dispatch({ type: types.PUT_MAPPINGS_SOURCE_BRANCH_STARTED });

    axios
      .put(
        `/mappings/source`,
        {},
        {
          headers: { 'content-type': 'application/json' },
          params: { branch: branch },
        }
      )
      .then((response) => {
        const sourceBranch = response.data;
        dispatch({ type: types.PUT_MAPPINGS_SOURCE_BRANCH_SUCCESS, payload: { sourceBranch } });
        onComplete(true);
      })
      .catch(() => {
        dispatch({ type: types.PUT_MAPPINGS_SOURCE_BRANCH_FAILURE });
        onComplete(false);
      });
  };
};

/**
 * Fetches the current source branch for the agent mappings file itself
 */
export const fetchMappingsSourceBranch = () => {
  return (dispatch) => {
    dispatch({ type: types.FETCH_MAPPINGS_SOURCE_BRANCH_STARTED });

    axios
      .get('/mappings/source')
      .then((response) => {
        const sourceBranch = response.data;
        dispatch({ type: types.FETCH_MAPPINGS_SOURCE_BRANCH_SUCCESS, payload: { sourceBranch } });
      })
      .catch(() => {
        dispatch({ type: types.FETCH_MAPPINGS_SOURCE_BRANCH_FAILURE });
      });
  };
};

/**
 * Shows or hides the history view.
 */
export const toggleHistoryView = () => ({
  type: types.TOGGLE_HISTORY_VIEW,
});
