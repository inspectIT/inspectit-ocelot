import * as types from "./types";
import axios from '../../../lib/axios-api';
import { notificationActions } from '../notification';

import { cloneDeep } from 'lodash';

/**
 * Fetches the agent mappings from the server.
 */
export const fetchMappings = () => {
  return dispatch => {
    dispatch({ type: types.FETCH_MAPPINGS_STARTED });

    axios
      .get("/mappings")
      .then(response => {
        const mappings = response.data;
        dispatch({ type: types.FETCH_MAPPINGS_SUCCESS, payload: { mappings } })
      })
      .catch(error => {
        dispatch({ type: types.FETCH_MAPPINGS_FAILURE })
      });
  };
};

/**
 * Stores the given agent mappings on the server. This mapping replaces the current agent mappings.
 * The given mappings will also be stored in the state when the request was successful.
 * 
 * @param {*} mappings - the agent mappings to store
 */
export const putMappings = (mappings, onComplete = () => { }) => {
  return dispatch => {
    dispatch({ type: types.PUT_MAPPINGS_STARTED });

    axios
      .put("/mappings", mappings, {
        headers: { "content-type": "application/json" }
      })
      .then(response => {
        dispatch({ type: types.PUT_MAPPINGS_SUCCESS, payload: { mappings } });
        dispatch(notificationActions.showSuccessMessage("Agent Mappings Saved", "The agent mappings have been successfully saved."));
        dispatch(fetchMappings());
        onComplete(true)
      })
      .catch(error => {
        dispatch({ type: types.PUT_MAPPINGS_FAILURE });
        onComplete(false)
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
export const putMapping = (mapping, onComplete = () => { }) => {
  return dispatch => {
    dispatch({ type: types.PUT_MAPPING_STARTED });

    axios
      .put(`/mappings/${mapping.name}`, mapping, {
        headers: { "content-type": "application/json" }
      })
      .then(response => {
        dispatch({ type: types.PUT_MAPPING_SUCCESS });
        dispatch(notificationActions.showSuccessMessage("Agent Mappings Saved", "The agent mappings have been successfully saved."));
        onComplete(true)

        dispatch(fetchMappings())
      })
      .catch(error => {
        dispatch({ type: types.PUT_MAPPING_FAILURE });
        onComplete(false)
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
  const nameToDelete = mapping.name ? mapping.name : mapping

  return (dispatch) => {
    dispatch({ type: types.DELETE_MAPPING_STARTED });

    axios
      .delete(`/mappings/${nameToDelete}`)
      .then(response => {
        dispatch({ type: types.DELETE_MAPPING_SUCCESS });
        dispatch(notificationActions.showSuccessMessage("Mapping Deleted", "The mapping has been successfully deleted."));
        dispatch(fetchMappings());
      })
      .catch(error => {
        dispatch({ type: types.DELETE_MAPPING_FAILURE });
      });
  };
}

