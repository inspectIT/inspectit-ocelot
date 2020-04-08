/* eslint react/jsx-no-target-blank: 0 */
import React from 'react';
import LoginCard from '../login/LoginCard';

/**
 * The login view, used by the login page.
 */
const LoginView = (props) => {
  return (
    <div className="this">
      <style jsx>
        {`
          .this {
            display: flex;
            justify-content: center;
            align-items: center;
            height: 100vh;
          }
          .build-information {
            position: absolute;
            bottom: 0.5rem;
            font-size: 0.75rem;
            color: #888;
          }
          .build-information a {
            color: #888;
          }
        `}
      </style>
      {props.children}
      <LoginCard />
      <div className="build-information">
        <span>
          inspectIT Ocelot Configuration Server v{process.env.VERSION} (Build Date: {process.env.BUILD_DATE}) |{' '}
          <a target="_blank" href="http://docs.inspectit.rocks/">
            Docs
          </a>{' '}
          |{' '}
          <a target="_blank" href="https://github.com/inspectIT/inspectit-ocelot">
            Github
          </a>
        </span>
      </div>
    </div>
  );
};

export default LoginView;
