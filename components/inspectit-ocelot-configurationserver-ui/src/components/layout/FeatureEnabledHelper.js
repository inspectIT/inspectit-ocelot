import { alertingEnabled } from '../views/alerting/alerting-api';

async function isFeatureEnabled(featureName, state) {
  switch (featureName) {
    case 'Alerting':
      return await alertingEnabled();
    case 'Settings':
      return state.authentication.permissions.admin;
    default:
      return true;
  }
}
export default isFeatureEnabled;
