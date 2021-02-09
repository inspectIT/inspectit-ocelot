import axiosBearer from '../../../../lib/axios-api';

/**
 * Generic fetch.
 */
export const fetch = (url) => {
  return axiosBearer.get(url).then((res) => res.data);
};

/**
 * Checks whether alerting is enabled.
 */
export const alertingEnabled = () => {
  return axiosBearer
    .get('/alert/kapacitor/')
    .then((res) => (res.data ? res.data.enabled === true : false))
    .catch(() => false);
};

/**
 * Checks whether alerting backend is online.
 */
export const kapacitorOnline = () => {
  return axiosBearer
    .get('/alert/kapacitor/')
    .then((res) => (res.data ? res.data.kapacitorOnline === true : false))
    .catch(() => false);
};
