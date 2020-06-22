import * as types from './types';
import { createReducer } from '../../utils';
import { dialog as initialState } from '../initial-states';

const dialogReducer = createReducer(initialState)({
  [types.SHOW_DIALOG]: (state, action) => {
    const { dialog, payload } = action.payload;
    return {
      ...state,
      show: dialog,
      payload,
    };
  },
  [types.HIDE_DIALOG]: (state) => {
    return {
      ...state,
      show: null,
      payload: null,
    };
  },
});

export default dialogReducer;
