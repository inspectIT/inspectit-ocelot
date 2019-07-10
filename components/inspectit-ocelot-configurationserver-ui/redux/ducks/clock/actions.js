import * as types from "./types";

export const initClock = (isServer) => ({
    type: types.INIT,
    payload: {
        isServer,
        ts: Date.now()
    }
});

export const tickClock = () => ({
    type: types.TICK,
    payload: {
        light: true,
        ts: Date.now()
    }
});

export const incrementCount = (value = 1) => ({
    type: types.INCREMENT,
    payload: { value }
});

export const decrementCount = () => ({
    type: types.DECREMENT
});

export const resetCount = () => ({
    type: types.RESET
});