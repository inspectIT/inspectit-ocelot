import { createSelector } from 'reselect'

const authenticationSelector = state => state.authentication;

/**
 * Returns whether the user is authenticated (logged in).
 */
export const isAuthenticated = createSelector(
    authenticationSelector,
    authentication => {
        return !!authentication.token;
    }
);