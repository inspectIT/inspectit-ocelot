---
id: syntax-highlighting
title: YAML Editor Syntax Highlighting
---

The *inspectIT Configuration Server* uses a custom [Mode for Syntax Highlighting](https://ace.c9.io/#nav=higlighter) in its Ace Editor based on the standard YAML Highlighting to help with writing your configurations.

## Additions to the standard YAML Highlighting

- Marking illegal fields that do not fit within the Java object the current part of the YAML corresponds to, which should help with both typing and indentation errors.
- Differentiating between keys that are predetermined by the underlying Java objects that are created from the YAML later and fields whose names you can choose yourself, e.g. the name of a scope.
- When a value in the YAML corresponds to an Enum, impossible values for that Enum are marked as invalid.
- Highlighting Java code in an action's value or value-body fields using Ace Editor's (albeit a bit limited) Java Highlighting.
  

| Examples of Syntax Highlighting      |   |
|--------------------------------------|---|
| Illegal keys / with typo             | ![Example of illegal field](assets/highlighting-example-wrongkey.png)  |
| Predetermined and freely chosen keys | ![Example of predetermined vs freely chosen field](assets/highlighting-example-chosenpredetermined-keys.png)  |
| Enum fields                          | ![Example of enum field](assets/highlighting-example-enums.png)  |
| Java before                          | ![Example of Java code with old highlighting](assets/highlighting-example-java-before.png)  |
| Java now                             | ![Example of Java code with new highlighting](assets/highlighting-example-java-after.png)  |

## Guidelines

The Syntax Highlighting is purely visual, so any previously working configuration will still work now, however it can happen that the Syntax Highlighting still marks parts of a technically correct configuration as invalid.
This happens because to make the Syntax Highlighting possible some rules needed to be established that the Highlighting can follow to understand and highlight the code correctly:

- Indentation between levels must always be 2 spaces, this also includes lists.
- JSON objects are only supported to a limited degree, i.e. when only one key-value pair is inside the JSON object. For cases with more than one key-value pair use nested YAML instead.
- Keys that are predetermined by the underlying Java objects and not freely chosen by you, must be written without quotation marks.
  

| Examples for guidelines      |                                                                                                |
|------------------------------|------------------------------------------------------------------------------------------------|
| Wrong indentation in general | ![Example of wrong indentation](assets/highlighting-example-wrongindentation.png)              |
| Wrong indentation in List    | ![Example of wrong indentation in List](assets/highlighting-example-wrongindentation-list.png) |
| Too big JSON objects         | ![Example of wrong usage of JSON object](assets/highlighting-example-JSONwrong.png)                                                                                               |
| Quotation marks around key   | ![Example of wrong quotation marks around key](assets/highlighting-example-quotationmarkswrong.png)                                                                                              |