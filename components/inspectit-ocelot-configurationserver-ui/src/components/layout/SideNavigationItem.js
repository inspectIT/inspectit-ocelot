import React from 'react';
import classNames from 'classnames';
import { withRouter } from 'next/router';
import Link from '../basics/Link';

/**
 * Component used by the side navigation as items.
 */
const SideNavigationItem = ({ href, icon, name, router, unfolded }) => {
  const isActive = router.pathname.endsWith(href) || router.pathname.includes(href + '/');

  return (
    <Link className="this" href={href}>
      <a className={classNames('anchor', { active: isActive, unfoldedSideNavItem: unfolded })} title={name}>
        <style jsx>
          {`
            .pi {
              font-size: 2rem;
              color: #bbb;
            }
            .anchor.active .pi,
            .anchor.active .name-text,
            .anchor:hover .pi,
            .anchor:hover .name-text {
              color: #888;
            }
            .anchor {
              width: 100%;
              text-align: center;
              padding: 0.75rem 0.25rem;
            }
            .anchor.active {
              padding-left: 0;
              border-left: 0.25rem solid #e8a034;
            }
            .unfoldedSideNavItem {
              display: flex;
              flex-direction: row;
              justify-content: flex-start;
              align-items: center;
              padding: 0.75rem 0.25rem;
            }
            .unfoldedSideNavItem i,
            .unfoldedSideNavItem .name-text {
              margin: 0 0.5rem 0;
              color: #bbb;
            }
          `}
        </style>
        <i className={'pi ' + icon} />
        {unfolded && <div className="name-text">{name}</div>}
      </a>
    </Link>
  );
};

export default withRouter(SideNavigationItem);
