import * as types from "./types";
import axios from '../../../lib/axios-api';
import { notificationActions } from '../notification';

import {cloneDeep} from 'lodash';

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
 * @param {*} resetEditor - if true, the YAML editors contents will be reset on success
 */
export const putMappings = (mappings, resetEditor) => {
    return dispatch => {
        dispatch({ type: types.PUT_MAPPINGS_STARTED });

        axios
            .put("/mappings", mappings, {
                headers: { "content-type": "application/json" }
            })
            .then(response => {
                dispatch({ type: types.PUT_MAPPINGS_SUCCESS, payload: { mappings, resetEditor } });
                dispatch(notificationActions.showSuccessMessage("Agent Mappings Saved", "The agent mappings have been successfully saved."));
            })
            .catch(error => {
                dispatch({ type: types.PUT_MAPPINGS_FAILURE });
            });
    };
};

/**
 * Replaces the current mapping copy. 
 * Won't affect original mappings or the data stored on the server.
 * 
 * @param {*} mappings - the mappings which will replace the current mappings copy
 */
export const replaceEditableMappings = (mappings) => ({
  type: types.UPDATE_MAPPING_IN_COPY,
  payload: {mappings}
});

/**
 * Removes the given mapping out of the mapping copy.
 * Won't affect original mappings or the data stored on the server.
 * 
 * @param {*} mapping - the mapping which should be deleted
 */
export const deleteEditableMapping = (mapping) => {
  return (dispatch, getState) => {
    const mappings = cloneDeep(getState().mappings.editableMappings)

    let indexToDelete;
    mappings.forEach((element, index) => {
      if(element === mapping) { indexToDelete = index; }
    });
  
    mappings.splice(indexToDelete, 1);
  
    dispatch({ type: types.UPDATE_MAPPING_IN_COPY, payload: {mappings} });
  }
}

/**
 * Replaces a single mapping within mapping copy.
 * 
 * @param {*} mapping - the mapping which will replace old mapping
 * @param {*} oldMapping - the mapping which will be replaced
 */
export const updateEditableMapping = (mapping, oldMapping) => {
  return (dispatch, getState) => {
    const mappings = cloneDeep(getState().mappings.editableMappings)
    
    let indexToUpdate;
    mappings.forEach((element, index) => {
      if(element.name === oldMapping.name) { indexToUpdate = index; }
    });

    mappings.splice(indexToUpdate, 1, mapping);

    dispatch({ type: types.UPDATE_MAPPING_IN_COPY, payload: {mappings} });
  }
}

/**
 * Inserts a single mapping at the beginning.
 * 
 * @param {*} newMapping - the new mapping which will be added
 */
export const addEditableMapping = (newMapping) => {
  return (dispatch, getState) => {
    const mappings = cloneDeep(getState().mappings.editableMappings)

    mappings.unshift(newMapping)
    dispatch({ type: types.UPDATE_MAPPING_IN_COPY, payload: {mappings} });
  }
}
