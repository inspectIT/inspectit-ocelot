import * as types from "./types";
import { createReducer } from "../../utils";
import {settings as initialState} from '../initial-states'

const settingsReducer = createReducer(initialState)({
  [types.CHANGE_PASSWORD_STARTED]: (state, action) => {
    return {
      ...state,
      loading: true
    }
  },
  [types.CHANGE_PASSWORD_FAILURE]: (state, action) => {
    return {
      ...state,
      loading: false,
    }
  },
  [types.CHANGE_PASSWORD_SUCCESS]: (state, action) => {
    return {
      ...state,
      loading: false,
    }
  },
  [types.SEARCH_USER_FAILURE]: (state, action) => {
    return {
      ...state,
      users: []
    }
  },
  [types.SEARCH_USER_SUCCESS]: (state, action) => {
    const {users} = action.payload 
    return {
      ...state,
      users: users,
    }
  },
})

export default settingsReducer