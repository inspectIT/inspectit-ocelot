import { alertingEnabled } from '../views/alerting/alerting-api';

export default async (featureName, state) => {
  switch (featureName) {
    case 'Alerting':
      return await alertingEnabled();
    case 'Settings':
      return state.authentication.permissions.admin;
    default:
      return true;
  }
};
