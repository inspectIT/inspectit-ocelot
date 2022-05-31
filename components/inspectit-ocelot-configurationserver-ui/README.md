# inspectIT Ocelot - Configuration Server UI

This is the web-frontend for the inspectIT Ocelot configuration server.

## How to

### Developing

Install [yarn](https://yarnpkg.com) on your system.

After that install all dependencies using yarn:
```bash
yarn
```

Using the following command, a development server can be started which serves the frontend and provides useful features like hot-reloading (changes will be immediatly updated in the browser - no refresh is required).
```bash
yarn dev
```

The development server can be reached at [http://localhost:3000](http://localhost:3000).

#### Testing

Using the following command, the front end tests can be run. This will execute 'jest'.

```bash
yarn test
```

or 

```bash
yarn test:watch
```

to run the tests in watch mode.

More information about how to test in [FRONTENDTESTS.md](FRONTENDTESTS.md)

#### Storybook

The project also contains [Storybook](https://storybook.js.org/) which supports the development of components by providing an isolated sandbox UI for testing these components.

Storybook can be started using the following command
```bash
yarn storybook
```

### Exporting

The frontend can be built and exported using the following command - the `build` task is included in the `export` task.

```bash
yarn export
```

The exported page will be located in the `out` directory.