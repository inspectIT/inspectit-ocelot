import React from 'react';
import PropTypes from 'prop-types';
import dateformat from 'dateformat';
import classnames from 'classnames';
import { RadioButton } from 'primereact/radiobutton';

/**
 * Single item representing a version in the version history panel.
 */
const VersionItem = ({ isSelected, onClick, isLatest, versionName, author, timestamp }) => {
  const classes = classnames('item', {
    selected: isSelected,
    latest: isLatest,
  });

  const modDate = dateformat(timestamp, 'yyyy-mm-dd');
  const modTime = dateformat(timestamp, 'HH:MM');

  return (
    <>
      <style jsx>
        {`
          .item {
            border-bottom: 1px solid #dddddd;
            cursor: pointer;
            transition: background-color 0.2s;
            padding: 0.3rem 0.3rem 0.3rem 1rem;
            display: flex;
            align-items: center;
          }
          .item:hover:not(.selected) {
            background: #eaeaea;
          }
          .item.latest {
            background-color: #fff7ea;
          }
          .selected {
            background: #eeeeee;
          }

          .details {
            flex: 1;
          }
          .item :global(.p-radiobutton) {
            margin-right: 1rem;
          }
          .upper-line {
            display: flex;
            align-items: center;
          }
          .version {
            flex: 1;
          }
          .version-id {
            font-family: monospace, monospace;
          }
          .latest-marker {
            color: #ef6c00;
            font-size: 0.9rem;
            font-style: italic;
          }
          .author {
            color: white;
            background-color: #007ad9;
            padding: 0 0.25rem;
            border-radius: 0.2rem;
            font-size: 0.9rem;
          }
          .lower-line {
            font-size: 0.75rem;
          }
          .lower-line span {
            color: #616161;
          }
        `}
      </style>

      <div className={classes} onClick={onClick}>
        <RadioButton checked={isSelected} />
        <div className="details">
          <div className="upper-line">
            <div className="version">
              <span className="version-id">{versionName}</span> {isLatest && <span className="latest-marker">&lt;Latest&gt;</span>}
            </div>
            {author && <div className="author">{author}</div>}
          </div>
          {timestamp && (
            <div className="lower-line">
              <span>authored</span> {modDate} @ {modTime}
            </div>
          )}
        </div>
      </div>
    </>
  );
};

VersionItem.propTypes = {
  /** the represented version */
  item: PropTypes.object,
  /** whether this version is currently selected */
  isSelected: PropTypes.bool,
  /** callback when on the element is clicked */
  onClick: PropTypes.func,
  /** whether this version is the latest version */
  isLatest: PropTypes.bool,
};

export default VersionItem;
