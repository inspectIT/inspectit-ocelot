import * as types from "./types";
import axios from '../../../lib/axios-api';


/**
 * Fetches the status of al lrecently connected agents.
 */
export const fetchStatus = () => {
    return dispatch => {
        dispatch({ type: types.FETCH_STATUS_STARTED });
        axios
            .get("/agentstatus")
            .then(res => {
                const agents = res.data;
                dispatch({
                    type: types.FETCH_STATUS_SUCCESS,
                    payload: { agents }
                });
            })
            .catch(() => {
                dispatch({ type: types.FETCH_STATUS_FAILURE });
            });
    };S
};

/**
 * Clears the status for all agents.
 */
export const clearStatus = (fetchStatusOnSuccess) => {
    return dispatch => {
        dispatch({ type: types.CLEAR_STATUS_STARTED });
        axios
            .delete("/agentstatus")
            .then(res => {
                dispatch({type: types.CLEAR_STATUS_SUCCESS});
                if(fetchStatusOnSuccess) {
                    dispatch(fetchStatus());
                }
            })
            .catch(() => {
                dispatch({ type: types.CLEAR_STATUS_FAILURE });
            });
    };S
};