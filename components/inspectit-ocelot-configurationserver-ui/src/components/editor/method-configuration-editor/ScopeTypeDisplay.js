import React from 'react';
import PropTypes from 'prop-types';
import HighlightText from './HighlightText';
import _ from 'lodash';

const MATCHER_MODE_DESCRIPTION = {
  EQUALS_FULLY: 'equals',
  EQUALS_FULLY_IGNORE_CASE: 'equals (case-insensitive)',
  STARTS_WITH: 'starts with',
  STARTS_WITH_IGNORE_CASE: 'starts with (case-insensitive)',
  ENDS_WITH: 'ends with',
  ENDS_WITH_IGNORE_CASE: 'ends with (case-insensitive)',
  CONTAINS: 'contains',
  CONTAINS_IGNORE_CASE: 'contains (case-insensitive)',
  MATCHES: 'matches',
};

const ScopeTypeDisplay = ({ scope }) => {
  const { type, superclass, interfaces } = scope;

  let target;
  let typeDescriptor;
  if (type) {
    target = type;
    typeDescriptor = <HighlightText value="Classes" />;
  } else if (superclass) {
    target = superclass;
    typeDescriptor = <HighlightText value="Classes with a superclass" />;
  } else if (interfaces && interfaces.length == 1) {
    target = interfaces[0];
    typeDescriptor = <HighlightText value="Classes implementing an interface" />;
  } else {
    //TODO
    throw new Error();
  }

  const { name } = target;

  const matcherMode = _.find(target, (_value, key) => {
    return key.toLowerCase() === 'matcher-mode';
  });

  const matcherText = _.get(MATCHER_MODE_DESCRIPTION, matcherMode, MATCHER_MODE_DESCRIPTION.EQUALS_FULLY);

  return (
    <span className="p-component">
      {typeDescriptor} whose name <HighlightText value={matcherText} theme="blue" /> <HighlightText value={name} />
    </span>
  );
};

ScopeTypeDisplay.propTypes = {
  scope: PropTypes.object,
};

ScopeTypeDisplay.defaultProps = {
  scope: {},
};

export default ScopeTypeDisplay;
