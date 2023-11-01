import * as types from './types';
import axios from '../../../lib/axios-api';
import { notificationActions } from '../notification';

/**
 * Fetches the agent mappings from the server.
 */
export const fetchMappings = () => {
  return (dispatch) => {
    dispatch({ type: types.FETCH_MAPPINGS_STARTED });

    axios
      .get('/mappings')
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
 * Send the new source branch for the agent mappings file itself
 * @param branch new source branch
 */
export const putMappingsSourceBranch = (branch, onComplete = () => {}) => {
  return (dispatch) => {
    dispatch({ type: types.PUT_MAPPING_STARTED });

    axios
      .put(
        `/mappings/source`,
        {},
        {
          headers: { 'content-type': 'application/json' },
          params: { branch: branch },
        }
      )
      .then(() => {
        dispatch({ type: types.PUT_MAPPING_SUCCESS });
        onComplete(true);
        //dispatch(fetchMappingsSourceBranch());
      })
      .catch(() => {
        dispatch({ type: types.PUT_MAPPING_FAILURE });
        onComplete(false);
      });
  };
};

/**
 * Fetches the current source branch for the agent mappings file itself
 */
export const fetchMappingsSourceBranch = () => {
  return (dispatch) => {
    dispatch({ type: types.FETCH_MAPPINGS_STARTED });

    axios
      .get('/mappings/source')
      .then((response) => {
        const currentBranch = response.data;
        dispatch({ type: types.FETCH_MAPPINGS_SUCCESS, payload: { currentBranch } });
      })
      .catch(() => {
        dispatch({ type: types.FETCH_MAPPINGS_FAILURE });
      });
  };
};
