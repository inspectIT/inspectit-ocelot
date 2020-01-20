import * as types from './types';
import axiosBearer from '../../../lib/axios-api';
import { notificationActions } from '../notification';

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

export const addUser = (userObj) => {
    return dispatch => {
        dispatch({ type: types.ADD_USER_STARTED });

        axiosBearer
            .post('/users', {
                username: userObj.username,
                password: userObj.password
            })
            .then(res => {
                dispatch({ type: types.ADD_USER_SUCCESS });

                const { id, username } = res.data;
                dispatch(notificationActions.showSuccessMessage('Request success', `User "${username}" been added. ID: ${id}`));
                dispatch(fetchUsers());
            })
            .catch(() => {
                dispatch({ type: types.ADD_USER_FAILURE });
            })
    }
}

export const deleteUser = (id) => {
    return dispatch => {
        dispatch({ type: types.DELETE_USER_STARTED });

        axiosBearer
            .delete(`/users/${id}`)
            .then(() => {
                dispatch({ type: types.DELETE_USER_SUCCESS });

                dispatch(notificationActions.showSuccessMessage('Request success', `User with ID: ${id} has been deleted`));
                dispatch(fetchUsers());
            })
            .catch(() => {
                dispatch({ type: types.DELETE_USER_FAILURE });
            })
    }
}