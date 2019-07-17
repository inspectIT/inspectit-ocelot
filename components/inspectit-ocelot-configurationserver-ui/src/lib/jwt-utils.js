import jwtDecode from 'jwt-decode';

export const isTokenExpired = (token) => {
    const { exp } = jwtDecode(token);
    const now = Date.now().valueOf() / 1000;
    return typeof exp !== 'undefined' && exp < now;
};