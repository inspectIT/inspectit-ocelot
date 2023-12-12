/* eslint default-param-last: 0 */
const createReducer =
  (initialState) =>
  (reducerMap) =>
  (state = initialState, action) => {
    const reducer = reducerMap[action.type];
    return reducer ? reducer(state, action) : state;
  };
export default createReducer;
