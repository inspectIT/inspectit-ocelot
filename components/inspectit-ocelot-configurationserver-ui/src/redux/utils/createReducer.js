/* eslint default-param-last: 0 */
export default (initialState) => (reducerMap) => (state = initialState, action) => {
  const reducer = reducerMap[action.type];
  return reducer ? reducer(state, action) : state;
};
