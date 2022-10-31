import { Provider } from 'react-redux';
import React from 'react';
import { configureStore } from '@reduxjs/toolkit';

/**
 * wrapper function for testing purposes that include a redux store
 */
export function storeWrapper(jsx, reducers) {
  const store = mockStore(reducers);
  return <Provider store={store}>{jsx}</Provider>;
}

/**
 * store configuration function that supplies a store with the provided reducers
 */
export function mockStore(reducers) {
  return configureStore({
    reducer: reducers,
    middleware: (getDefaultMiddleware) => getDefaultMiddleware({ serializableCheck: false }),
  });
}
