import jwtDecode from 'jwt-decode';

/**
 * Checks whether the given token is already expired.
 * The function returns false if the token does not contain an expiration date.
 * This function does not handle time sync issues between the server and client!
 *
 * @param {string} token
 */
export const isTokenExpired = (token) => {
  const { exp } = jwtDecode(token);
  const now = Date.now().valueOf() / 1000;
  return typeof exp !== 'undefined' && exp < now;
};
