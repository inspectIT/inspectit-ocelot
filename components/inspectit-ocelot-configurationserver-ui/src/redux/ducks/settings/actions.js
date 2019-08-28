import * as types from './types'
import {axiosPlain} from '../../../lib/axios-api'
import axiosBearer from '../../../lib/axios-api'
import { notificationActions } from '../notification'

export const changePassword = (username, oldPassword, newPassword) => {
  return dispatch => {
    dispatch({ type: types.CHANGE_PASSWORD_STARTED })

    axiosPlain
      .put('/account/password', {
          password : newPassword
        },{
        auth: {
          username: username,
          password: oldPassword
        }
      })
      .then(res => { 
        dispatch({ type: types.CHANGE_PASSWORD_SUCCESS }) 
        dispatch(notificationActions.showSuccessMessage('Request failed', 'Your password has been changed'))
      })
      .catch(e => { 
        const {response} = e
        if(response && response.status === 401) {
          dispatch(notificationActions.showErrorMessage('Request failed', 'the given password was wrong'))
        } else {
          dispatch(notificationActions.showErrorMessage('Request failed', e))
        }
        dispatch({ type: types.CHANGE_PASSWORD_FAILURE }) 
      })
  }
}

export const fetchUsers = () => {
  return dispatch => {
    axiosBearer
      .get(`/users`)
      .then(res => {
        dispatch({ type: types.SEARCH_USER_SUCCESS, payload: { users: res.data } })
       })
      .catch(e => {
        dispatch({ type: types.SEARCH_USER_FAILURE })
      })
  }
}

export const deleteUser = (id) =>{
  return dispatch => {
    axiosBearer
      .delete(`/users/${id}`)
      .then(() => { 
        dispatch(fetchUsers())
        dispatch(notificationActions.showSuccessMessage('Request success', `User with ID: ${id} has been deleted`))
      })
  }
}

export const addUser = (userObj) => {
  return dispatch => {
    axiosBearer
      .post('/users', {
        username: userObj.username,
        password: userObj.password
      })
      .then(res => {
        const {id, username} = res.data
        const message = `User "${username}" with ID: ${id} has been added`
        dispatch(fetchUsers())
        dispatch(notificationActions.showSuccessMessage('Request success', message))
      })
  }
}