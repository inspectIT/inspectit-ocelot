import { createSelector } from 'reselect';
import jwtDecode from 'jwt-decode';

const authenticationSelector = (state) => state.authentication;

/**
 * Returns whether the user is authenticated (logged in).
 */
export const isAuthenticated = createSelector(authenticationSelector, (authentication) => {
  return !!authentication.token;
});

/**
 * Returns the expiring date of the current access token.
 */
export const getTokenExpirationDate = createSelector(authenticationSelector, (authentication) => {
  if (authentication.token) {
    return jwtDecode(authentication.token).exp;
  } else {
    return null;
  }
});
