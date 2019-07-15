import { createSelector } from 'reselect'

const clockSelector = state => state.clock;

export const isNegativeCount = createSelector(
    clockSelector,
    clock => {
        return clock.count < 0;
    }
);