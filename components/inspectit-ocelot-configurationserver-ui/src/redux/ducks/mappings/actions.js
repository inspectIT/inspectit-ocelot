import * as types from "./types";
import axios from '../../../lib/axios-api';

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

export const putMappings = (mappings) => {
    return dispatch => {
        dispatch({ type: types.PUT_MAPPINGS_STARTED });

        axios
            .put("/mappings", mappings, {
                headers: { "content-type": "application/json" }
            })
            .then(response => {
                dispatch({ type: types.PUT_MAPPINGS_SUCCESS, payload: { mappings } });
            })
            .catch(error => {
                dispatch({ type: types.PUT_MAPPINGS_FAILURE });
            });
    };
};