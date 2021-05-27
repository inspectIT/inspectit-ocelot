import React from 'react';
import PropTypes from 'prop-types';
import HighlightText from './HighlightText';
import _ from 'lodash';

const MATCHER_MODE_DESCRIPTION = {
  EQUALS_FULLY: 'equal',
  EQUALS_FULLY_IGNORE_CASE: 'equal (case-insensitive)',
  STARTS_WITH: 'start with',
  STARTS_WITH_IGNORE_CASE: 'start with (case-insensitive)',
  ENDS_WITH: 'end with',
  ENDS_WITH_IGNORE_CASE: 'end with (case-insensitive)',
  CONTAINS: 'contain',
  CONTAINS_IGNORE_CASE: 'contain (case-insensitive)',
  MATCHES: 'match',
};

const findIgnoreCase = (object, findKey) => {
  return _.find(object, (_value, key) => {
    return key.toLowerCase() === findKey;
  });
};

const ScopeMethodDisplay = ({ scope }) => {
  const { methods } = scope;

  if (!methods || methods.length === 0) {
    return <span className="p-component">All existing methods</span>;
  }

  if (methods.length !== 1) {
    //TODO
    throw new Error('Not supported');
  }

  const method = methods[0];

  // method type
  let methodTypeDescriptor;
  let nameDescriptor;
  const constructor = findIgnoreCase(method, 'is-constructor');
  if (constructor) {
    methodTypeDescriptor = <>Constructors</>;
  } else {
    // name
    methodTypeDescriptor = <>Methods</>;
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

  // visibility
  let visibilityDescriptor;
  const visibility = findIgnoreCase(method, 'visibility');
  if (!_.isEmpty(visibility)) {
    const anyVisibility = _.isEmpty(_.difference(['PUBLIC', 'PROTECTED', 'PACKAGE', 'PRIVATE'], visibility));

    if (!anyVisibility) {
      const visibilities = _.join(visibility, ' OR ');
      visibilityDescriptor = (
        <>
          with visibility of <HighlightText value={visibilities} theme="green" />
        </>
      );
    }
  }
  //   } else {
  //     visibilityDescriptor = <>with any visibility</>;
  //   }

  // arguments
  let argumentDescriptor;
  const methodArguments = findIgnoreCase(method, 'arguments');
  if (_.isNil(methodArguments)) {
    //argumentDescriptor = <>with any arguments</>;
  } else if (_.isEmpty(methodArguments)) {
    argumentDescriptor = <>without arguments</>;
  } else {
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
  scope: PropTypes.object,
};

ScopeMethodDisplay.defaultProps = {
  scope: {},
};

export default ScopeMethodDisplay;
