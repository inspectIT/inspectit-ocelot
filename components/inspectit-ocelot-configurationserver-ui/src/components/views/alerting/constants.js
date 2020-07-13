import classNames from 'classnames';

export const templateIcon = 'pi-briefcase';
export const ruleIcon = 'pi-bell';
export const topicIcon = 'pi-bars';

/** prime icon names for different handler kinds */
export const handlerIcons = (kind) => {
  if (!kind) {
    return 'pi-circle-on';
  }
  switch (kind) {
    case 'smtp':
      return 'pi-envelope';
    case 'publish':
      return 'pi-sign-in';
    default:
      return 'pi-circle-on';
  }
};

/** prime icon names for different topic severity levels */
export const severityLevelClassNames = (severityLevel, isReferenceOnly) => {
  if (isReferenceOnly || !severityLevel) {
    return classNames('pi-eye', 'grey');
  }
  switch (severityLevel) {
    case 'OK':
      return classNames('pi-check-circle', 'green');
    case 'INFO':
      return classNames('pi-info-circle', 'blue');
    case 'WARNING':
      return classNames('pi-exclamation-triangle', 'orange');
    case 'CRITICAL':
      return classNames('pi-exclamation-triangle', 'red');
    default:
      return classNames('pi-circle-on', 'grey');
  }
};

export const supportedHandlerTypes = ['smtp', 'publish'];
