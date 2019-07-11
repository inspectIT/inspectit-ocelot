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

### Exporting

The frontend can be built and exported using the following command - the `build` task is included in the `export` task.

```bash
yarn export
```

The exported page will be located in the `out` directory.