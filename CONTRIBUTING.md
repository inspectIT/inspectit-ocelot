# Contributing

## IDE

We recommend using [IntelliJ](https://www.jetbrains.com/idea/download/#section=windows) as IDE for contributing.

We also recommend installing the **Gradle Plugin** and the **[Save Actions Plugin](https://plugins.jetbrains.com/plugin/7642-save-actions)** (Note that you need to make sure your [keymap is up to date](https://youtrack.jetbrains.com/issue/IDEA-277194) for the plugin to work properly).

For IntelliJ 203.* or older, you also need to manually install the **[Lombok Plugin](https://plugins.jetbrains.com/plugin/6317-lombok-plugin)**. This step is not needed for later versions as they already ship with the plugin. 

After installing IntelliJ you can import the root folder of this repository as a Gradle project. 
When using the Save Actions Plugin, you have to copy the *saveactions_settings.xml* from the `/codequality/idea` to the  `.idea` directory which is generated on project import.

Furthermore, please import the `/codequality/idea/code_style.xml` file into the project's settings of IntelliJ in order to match the code style we're using.

### Frontend Developing

If developing JavaScript in IntelliJ Ultimate, check that Settings.Languages & Frameworks.JavaScript.Code Quality Tools.ESLint is activated for correct linting.

In the same location, check the checkbox for running `eslint --fix on save`.

## Process

The first step in order to contribute is to fork this repository into your account.
You can develop your changes on a separate branch there and open a [pull request](https://github.com/inspectIT/inspectit-ocelot/pulls)
in the main repository as soon as you are done.

> **Important**: please update and commit the third party licenses (`THIRD-PARTY-LICENSES.txt`) if you changed any of the `build.gradle` files by executing the gradle task `gradlew generateLicenseReport` before you open your pull request.

Usually, a pull request should correspond to an open [issue](https://github.com/inspectIT/inspectit-ocelot/issues) in this repository.
Therefore, make sure that an issue exists which your pull request attempts to resolve. If none exists, you should create one.
There is an exemption to this rule: For very small fixes (e.g. Typo-Fixes), your pull request does not need to have a corresponding issue.
If your PR corresponds to an issue, the issue should be correctly labeled (e.g. `enhancement`, `bug`, etc.) because the release notes will be generated based on these labels.

When you open your pull request, you should use the following format for the title:

`Closes #{Issue ID} - {Description} (#{Pull Request ID})`

Members from the inspectIT team will then review your changes and either request changes or approve your PR.
In case changed were requested, please fix them (or discuss the changes) and then your PR will be reviewed again.

As soon as everything is approved, the inspectIT admins will merge your pull request.
Your change will become a single commit with the title of the PR as commit message.
