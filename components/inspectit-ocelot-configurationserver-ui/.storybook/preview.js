import React from 'react';

// styles for primereact
import 'primereact/resources/themes/nova-dark/theme.css';
import 'primereact/resources/primereact.min.css';
import 'primeicons/primeicons.css';
import 'primeflex/primeflex.css';

// global decorator for styles from '_app.js'
export const decorators = [
  (Story) => (
    <div>
      <style global jsx>
        {`
          body {
            margin: 0;
            font-family: 'Open Sans', 'Helvetica Neue', sans-serif;
          }
        `}
      </style>
      <Story />
    </div>
  ),
];

export const parameters = {
  actions: { argTypesRegex: '^on[A-Z].*' },
  controls: {
    matchers: {
      color: /(background|color)$/i,
      date: /Date$/,
    },
  },
};
