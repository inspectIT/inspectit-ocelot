import * as types from "./types";

export const serverRenderClock = () => ({
    type: types.TICK,
    light: false,
    ts: Date.now()
});

export const startClock = () => ({
    type: types.TICK,
    light: true,
    ts: Date.now()
});

export const incrementCount = () => ({
    type: types.INCREMENT
});

export const decrementCount = () => ({
    type: types.DECREMENT
});

export const resetCount = () => ({
    type: types.RESET
});