import { createSelector } from 'reselect'

const authenticationSelector = state => state.authentication;

export const isAuthenticated = createSelector(
    authenticationSelector,
    authentication => {
        return !!authentication.token;
    }
);