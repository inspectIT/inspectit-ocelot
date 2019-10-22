# Contributing

## IDE

We recommend using [IntelliJ](https://www.jetbrains.com/idea/download/#section=windows) as IDE for contributing.

We also recommend installing the **Gradle Plugin**, the **[Save Actions Plugin](https://plugins.jetbrains.com/plugin/7642-save-actions)** and the **[Lombok Plugin](https://plugins.jetbrains.com/plugin/6317-lombok-plugin)** for IntelliJ.

After installing IntelliJ you can import the root folder of this repository as a Gradle project. 
When using the Save Actions Plugin, you have to copy the *saveactions_settings.xml* from the `/codequality/idea` to the  `.idea` directory which is generated on project import.

Furthermore, please import the `/codequality/idea/code_style.xml` file into the project's settings of IntelliJ in order to match the code style we're using.

## Commit Message Policy

If you are committing a change which refers to a certain issue, please reference the issue by using the following notation:

`Closes #{Issue ID} - {Issue Description} (#{Pull Request ID})`

A example would be: `Closes #1 - Add README (#2)`