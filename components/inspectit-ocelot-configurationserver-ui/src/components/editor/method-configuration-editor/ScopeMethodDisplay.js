import React from 'react';
import PropTypes from 'prop-types';
import HighlightText from './HighlightText';
import _ from 'lodash';
import { MATCHER_MODE_DESCRIPTION, DEFAULT_VISIBILITIES } from './constants';

/**
 * Utility function for finding (case-insensitive) a specific attribute of a given object.
 *
 * @param {*} object  the object being searched
 * @param {*} findKey the name of the attribute to find
 */
const findIgnoreCase = (object, findKey) => {
  return _.find(object, (_value, key) => {
    return key.toLowerCase() === findKey;
  });
};

/**
 * Component for displaying the method matchers of given scope in a nice representation.
 */
const ScopeMethodDisplay = ({ scope }) => {
  const { methods } = scope;

  // In case no method matcher exists, all methods will be used.
  if (!methods || methods.length === 0) {
    return <span className="p-component">All existing methods</span>;
  }

  // The editor does not support scopes targeting multiple methods.
  // Individual scopes must be used for this purpose.
  if (methods.length !== 1) {
    throw new Error('Multi-method scopes are currently not supported.');
  }

  // the method matcher which will be shown
  const method = methods[0];

  // defining the visual elements for the matcher's type and name
  let methodTypeDescriptor;
  let nameDescriptor;
  const constructor = findIgnoreCase(method, 'is-constructor');
  if (constructor) {
    // in case constructors should be matched
    methodTypeDescriptor = <>Constructors</>;
    // nameDescriptor not needed for constructors
  } else {
    // in case methods should be matched
    methodTypeDescriptor = <>Methods</>;
    // show name only if defined
    const name = findIgnoreCase(method, 'name');
    if (name) {
      const matcherMode = findIgnoreCase(method, 'matcher-mode');
      const matcherText = _.get(MATCHER_MODE_DESCRIPTION, matcherMode, MATCHER_MODE_DESCRIPTION.EQUALS_FULLY);

      nameDescriptor = (
        <>
          whose names <HighlightText value={matcherText} theme="blue" /> <HighlightText value={name} />
        </>
      );
    }
  }

  // defining the visual elements for the matcher's visibility
  let visibilityDescriptor;
  const visibility = findIgnoreCase(method, 'visibility');
  // show visibility only if defined
  if (!_.isEmpty(visibility)) {
    const isDefaultVisibility = _.isEmpty(_.difference(DEFAULT_VISIBILITIES, visibility));

    // show visibility only when it differs from the default values
    if (!isDefaultVisibility) {
      const visibilities = _.join(visibility, ' OR ');
      visibilityDescriptor = (
        <>
          with visibilities of <HighlightText value={visibilities} theme="green" />
        </>
      );
    } else if (!nameDescriptor) {
      visibilityDescriptor = <>with any visibility</>;
    }
  }

  // defining the visual elements for the matcher's arguments
  let argumentDescriptor;
  const methodArguments = findIgnoreCase(method, 'arguments');
  if (_.isNil(methodArguments)) {
    // show nothing when it is not defined
  } else if (_.isEmpty(methodArguments)) {
    // in case an empty array is specified
    argumentDescriptor = <>without arguments</>;
  } else {
    // in case arguments are specified - join them together
    const argumentsString = _.join(methodArguments, ', ');
    argumentDescriptor = (
      <>
        with arguments <HighlightText value={argumentsString} theme="yellow" />
      </>
    );
  }

  return (
    <span className="p-component">
      {methodTypeDescriptor} {nameDescriptor} {visibilityDescriptor} {argumentDescriptor}
    </span>
  );
};

ScopeMethodDisplay.propTypes = {
  /** The scopes to visualize. */
  scope: PropTypes.object,
};

ScopeMethodDisplay.defaultProps = {
  scope: {},
};

export default ScopeMethodDisplay;
