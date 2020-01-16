import * as types from './types';
import axiosBearer from '../../../lib/axios-api';

export const fetchUsers = () => {
    return dispatch => {
        dispatch({ type: types.FETCH_USERS_STARTED });

        axiosBearer
            .get(`/users`)
            .then(res => {
                dispatch({ type: types.FETCH_USERS_SUCCESS, payload: { users: res.data } });
            })
            .catch(() => {
                dispatch({ type: types.FETCH_USERS_FAILURE });
            })
    }
} 