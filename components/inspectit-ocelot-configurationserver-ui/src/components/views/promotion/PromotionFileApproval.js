import classnames from 'classnames';
import _ from 'lodash';
import { ToggleButton } from 'primereact/togglebutton';
import PropTypes from 'prop-types';
import React from 'react';

/**
 * The footer bar used by users to approve the currently selected promotion file.
 */
const PromotionFileApproval = ({ currentUser, authors, canApprove, approved, onApproveFile, allowSelfApproval }) => {
  const preventApproval = !allowSelfApproval && authors.includes(currentUser);

  return (
    <>
      <style jsx>
        {`
          .this {
            border-top: 1px solid #ddd;
            display: flex;
            padding: 0.5rem 1rem;
            flex-direction: row-reverse;
          }
          .users {
            flex-grow: 1;
            display: flex;
            align-items: center;
          }
          .user {
            color: white;
            background-color: #007ad9;
            padding: 0.1rem 0.25rem;
            margin-right: 0.5rem;
            border-radius: 0.2rem;
          }
          .description {
            margin-right: 1rem;
          }
          .current-user {
            color: #131313;
            background-color: #d0d0d0;
          }
        `}
      </style>

      <div className="this p-component">
        {canApprove &&
          (!preventApproval ? (
            <ToggleButton
              onLabel="Approved"
              offLabel="Not Approved"
              onIcon="pi pi-check"
              offIcon="pi pi-times"
              checked={approved}
              onChange={onApproveFile}
            />
          ) : (
            <div>You cannot promote files that contain changes made by yourself.</div>
          ))}

        {!_.isEmpty(authors) && (
          <div className="users">
            <span className="description">File has been modified by users:</span>
            {authors.map((author) => (
              <span key={author} className={classnames('user', { 'current-user': author.toLowerCase() === currentUser.toLowerCase() })}>
                {author}
              </span>
            ))}
          </div>
        )}
      </div>
    </>
  );
};

PromotionFileApproval.propTypes = {
  /** The name of the currently logged in user */
  currentUser: PropTypes.string,
  /** Array of users which modified the currently selected file */
  authors: PropTypes.arrayOf(PropTypes.string),
  /** Whether the use can approve files */
  canApprove: PropTypes.bool,
  /** Whether the current file is approved */
  approved: PropTypes.bool,
  /** Callback when the approve the approval state should be toggled */
  onApproveFile: PropTypes.func,
  /** Whether the user can approve its own changes */
  allowSelfApproval: PropTypes.bool,
};

export default PromotionFileApproval;
