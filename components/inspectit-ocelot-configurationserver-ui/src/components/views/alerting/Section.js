import React from 'react';
import PropTypes from 'prop-types';
import classNames from 'classnames';

const Section = ({ title, children, scrollable }) => {
  return (
    <div className={classNames("this", { 'scrollable': scrollable })}>
      <style jsx>{`
          .this {
            display: flex;
            flex-direction: column;
            justify-content: flex-start;
            margin: 1rem;
          }
          .this :global(.section-container) {
            border: 1px solid #c8c8c8;
            display: flex;
            flex-direction: column;
          }
          .this :global(.section-header) {
            padding: 0.5rem 1rem 0.5rem;
            background-color: #eee;
            border: 1px solid #c8c8c8;
            font-weight: bold;
          }
          .scrollable {
            overflow-y: auto;
          }
      `}</style>
      <div className="section-header">{title}</div>
      <div className={classNames("section-container", { 'scrollable': scrollable })}>
        {children}
      </div>
    </div>
  );
};

Section.propTypes = {
  /** Title of the section */
  title: PropTypes.string.isRequired,
  /**  List of child elements */
  children: PropTypes.object,
  /** Whether the section should be scrollable */
  scrollable: PropTypes.bool
};

Section.defaultProps = {
  children: [],
  scrollable: false,
};

export default Section;
