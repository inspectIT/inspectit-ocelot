import * as types from "./types";

export const serverRenderClock = () => ({
    type: actionTypes.TICK,
    light: false,
    ts: Date.now()
});

export const startClock = () => ({
    type: actionTypes.TICK,
    light: true,
    ts: Date.now()
});

export const incrementCount = () => ({
    type: actionTypes.INCREMENT
});

export const decrementCount = () => ({
    type: actionTypes.DECREMENT
});

export const resetCount = () => ({
    type: actionTypes.RESET
});

export function initializeStore(initialState = exampleInitialState) {
    return createStore(
        reducer,
        initialState,
        composeWithDevTools(applyMiddleware())
    )
}
