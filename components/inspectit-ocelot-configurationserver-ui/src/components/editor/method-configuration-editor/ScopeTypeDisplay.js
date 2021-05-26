import React from 'react';
import PropTypes from 'prop-types';
import HighlightText from './HighlightText';

const ScopeTypeDisplay = ({ scope }) => {
  const { type, superclass, interfaces } = scope;

  let target;
  let typeDescriptor;
  console.log(scope);
  if (type) {
    target = type;
    typeDescriptor = 'Class';
  } else if (superclass) {
    target = superclass;
    typeDescriptor = 'Superclass';
  } else if (interfaces && interfaces.length == 1) {
    target = interfaces[0];
    typeDescriptor = 'Interface';
  } else {
    //TODO
    throw new Error();
  }

  const {name} = target;

  return (
    <span className="p-component">
      {typeDescriptor} with name <HighlightText value={name} />
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
