import * as types from "./types";
import axios from '../../../lib/axios-api';
import { notificationActions } from '../notification';

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
                dispatch({ type: types.FETCH_MAPPINGS_SUCCESS, payload: { mappings } });
            })
            .catch(error => {
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
export const putMappings = (mappings) => {
    return dispatch => {
        dispatch({ type: types.PUT_MAPPINGS_STARTED });

        axios
            .put("/mappings", mappings, {
                headers: { "content-type": "application/json" }
            })
            .then(response => {
                dispatch({ type: types.PUT_MAPPINGS_SUCCESS, payload: { mappings } });
                dispatch(notificationActions.showSuccessMessage("Agent Mappings Saved", "The agent mappings have been successfully saved."));
            })
            .catch(error => {
                dispatch({ type: types.PUT_MAPPINGS_FAILURE });
            });
    };
};