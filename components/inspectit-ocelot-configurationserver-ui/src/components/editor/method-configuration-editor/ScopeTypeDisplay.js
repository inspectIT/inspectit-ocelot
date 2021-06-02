import React from 'react';
import PropTypes from 'prop-types';
import HighlightText from './HighlightText';
import _ from 'lodash';
import { MATCHER_MODE_DESCRIPTION_S } from './constants';

/**
 * Component for displaying the type matchers of given scope in a nice representation.
 */
const ScopeTypeDisplay = ({ scope }) => {
  const { type, superclass, interfaces } = scope;

  let targetMatcher;
  let typeDescriptor;
  if (type) {
    targetMatcher = type;
    typeDescriptor = <HighlightText value="Classes" />;
  } else if (superclass) {
    targetMatcher = superclass;
    typeDescriptor = <HighlightText value="Classes with a superclass" />;
  } else if (interfaces && interfaces.length == 1) {
    targetMatcher = interfaces[0];
    typeDescriptor = <HighlightText value="Classes implementing an interface" />;
  } else {
    // Scopes with multiple type matchers are currently not supported.
    throw new Error('Scopes using multiple type matchers are currently not supported.');
  }

  const { name } = targetMatcher;

  const matcherMode = _.find(targetMatcher, (_value, key) => {
    return key.toLowerCase() === 'matcher-mode';
  });
  const matcherText = _.get(MATCHER_MODE_DESCRIPTION_S, matcherMode, MATCHER_MODE_DESCRIPTION_S.EQUALS_FULLY);

  return (
    <span className="p-component">
      {typeDescriptor} whose name <HighlightText value={matcherText} theme="blue" /> <HighlightText value={name} />
    </span>
  );
};

ScopeTypeDisplay.propTypes = {
  /** The scopes to visualize. */
  scope: PropTypes.object,
};

ScopeTypeDisplay.defaultProps = {
  scope: {},
};

export default ScopeTypeDisplay;
