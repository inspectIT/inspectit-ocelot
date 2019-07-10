import { createStore, applyMiddleware, combineReducers } from "redux";
import { composeWithDevTools } from 'redux-devtools-extension'
import * as reducers from "./ducks";
import { createLogger } from "./middlewares";

export default function configureStore(initialState) {
    const rootReducer = combineReducers(reducers);

    return createStore(
        rootReducer,
        initialState,
        composeWithDevTools(applyMiddleware(
            createLogger(true)
        ))
    );
}
