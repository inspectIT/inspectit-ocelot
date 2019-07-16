const isProduction = process.env.NODE_ENV === 'production';

/**
 * Various constants used by the application.
 */
export const BASE_PAGE_TITLE = "inspectIT Ocelot Configuration Server";

/**
 * Base URL of the backend api v1.
 */
export const BASE_API_URL_V1 = isProduction ? "/api/v1" : "http://localhost:8090/api/v1";