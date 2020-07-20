import React from 'react';
import PropTypes from 'prop-types';
import { Card } from 'primereact/card';

const Section = ({ title, children, backgroundColor }) => {
  return (
    <>
      <style jsx>{`
        :global(.section.p-card) {
          margin: 1rem;
        }
        :global(.section .p-card-title) {
          font-size: 1.1rem;
        }
      `}</style>

      <Card className="section" title={title} style={{ backgroundColor: backgroundColor }}>
        {children}
      </Card>
    </>
  );
};

Section.propTypes = {
  /** Title of the section */
  title: PropTypes.string.isRequired,
  /**  List of child elements */
  children: PropTypes.object,

  backgroundColor: PropTypes.string,
};

Section.defaultProps = {
  children: [],
};

export default Section;
