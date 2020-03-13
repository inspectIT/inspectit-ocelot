import { createStore, applyMiddleware, combineReducers } from "redux";
import { composeWithDevTools } from 'redux-devtools-extension'
import * as reducers from "./ducks";
import { createLogger, schemaFetcher } from "./middlewares";
import thunk from 'redux-thunk';
import { persistReducer } from 'redux-persist'
import storage from 'redux-persist/lib/storage'

export default function configureStore(initialState, isServer) {
    let reducer = combineReducers(reducers);

    if (!isServer) {
        const persistConfig = {
            key: 'primary',
            storage,
            whitelist: ['authentication']
        }

        reducer = persistReducer(persistConfig, reducer);
    }

    return createStore(
        reducer,
        initialState,
        composeWithDevTools(applyMiddleware(
            createLogger(true),
            schemaFetcher,
            thunk
        ))
    );
}
