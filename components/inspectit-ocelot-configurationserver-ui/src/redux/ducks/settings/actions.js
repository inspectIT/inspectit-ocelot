import * as types from './types';
import axiosBearer, { axiosPlain } from '../../../lib/axios-api';
import { notificationActions } from '../notification';

export const fetchUsers = () => {
  return (dispatch) => {
    dispatch({ type: types.FETCH_USERS_STARTED });

    axiosBearer
      .get(`/users`)
      .then((res) => {
        dispatch({ type: types.FETCH_USERS_SUCCESS, payload: { users: res.data } });
      })
      .catch(() => {
        dispatch({ type: types.FETCH_USERS_FAILURE });
      });
  };
};

export const addUser = (userObj) => {
  return (dispatch) => {
    dispatch({ type: types.ADD_USER_STARTED });

    axiosBearer
      .post('/users', {
        username: userObj.username,
        password: userObj.password,
      })
      .then((res) => {
        dispatch({ type: types.ADD_USER_SUCCESS });

        const { id, username } = res.data;
        dispatch(notificationActions.showSuccessMessage('Request Success', `User "${username}" has been added. ID: ${id}`));
        dispatch(fetchUsers());
      })
      .catch(() => {
        dispatch({ type: types.ADD_USER_FAILURE });
      });
  };
};

export const deleteUser = (id) => {
  return (dispatch) => {
    dispatch({ type: types.DELETE_USER_STARTED });

    axiosBearer
      .delete(`/users/${id}`)
      .then(() => {
        dispatch({ type: types.DELETE_USER_SUCCESS });

        dispatch(notificationActions.showSuccessMessage('Request Success', `User with ID: ${id} has been deleted`));
        dispatch(fetchUsers());
      })
      .catch(() => {
        dispatch({ type: types.DELETE_USER_FAILURE });
      });
  };
};

export const changePassword = (username, oldPassword, newPassword) => {
  return (dispatch) => {
    dispatch({ type: types.CHANGE_PASSWORD_STARTED });

    axiosPlain
      .put(
        '/account/password',
        {
          password: newPassword,
        },
        {
          auth: {
            username: username,
            password: oldPassword,
          },
        }
      )
      .then(() => {
        dispatch({ type: types.CHANGE_PASSWORD_SUCCESS });
        dispatch(notificationActions.showSuccessMessage('Request Success', 'Your password has been changed'));
      })
      .catch((e) => {
        const { response } = e;
        if (response && response.status === 401) {
          dispatch(notificationActions.showErrorMessage('Request Failed', 'The given password was wrong'));
        } else if (response && response.data && response.data.message) {
          dispatch(notificationActions.showErrorMessage('Request Failed', response.data.message));
        } else {
          dispatch(notificationActions.showErrorMessage('Request Failed', e.message));
        }
        dispatch({ type: types.CHANGE_PASSWORD_FAILURE });
      });
  };
};
