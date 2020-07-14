export const descriptionScope = () => { return (
<div class="scope9001"> 
<div class="container mainContainer docsContainer XXsnipcss_extracted_selector_selectionXX">
  <div class="wrapper">
    <div class="post">
      <header class="postHeader">
        <a class="edit-page-link button" href="https://github.com/inspectit/inspectit-ocelot-documentation/edit/master/docs/instrumentation/scopes.md" target="_blank" rel="noreferrer noopener">
          Edit
        </a>
        <h1 id="__docusaurus" class="postHeaderTitle">
          Scopes
        </h1>
      </header>
      <article>
        <div>
          <span>
            <p>
              A Scope is one of the basic instrumentation configuration components.
              It is important to get familiar with it in order to understand which methods are targeted by which instrumentation rule.
              A single scope can be considered as a set of methods and is used by rules to determine which instrumentation should apply to which method.
            </p>
            <p>
              Scopes are defined under the configuration key 
              <code>
                inspectit.instrumentation.scopes
              </code>
              and will be defined as a map of key-value pairs:
            </p>
            <pre>
<code class="hljs css language-yaml">
<span class="hljs-attr">
inspectit:
</span>
<span class="hljs-attr">
instrumentation:
</span>
<span class="hljs-attr">
scopes:
</span>
<span class="hljs-attr">
's_my_scope':
</span>
<span class="hljs-comment">
# SCOPE_DEFINITION
</span>
<span class="hljs-attr">
's_another_scope':
</span>
<span class="hljs-comment">
# SCOPE_DEFINITION
</span>
</code>
</pre>
            <h2>
              <a class="anchor" aria-hidden="true" id="scope-definition">
              </a>
              <a href="#scope-definition" aria-hidden="true" class="hash-link">
                <svg class="hash-link-icon" aria-hidden="true" height="16" version="1.1" viewBox="0 0 16 16" width="16">
                  <path fill-rule="evenodd" d="M4 9h1v1H4c-1.5 0-3-1.69-3-3.5S2.55 3 4 3h4c1.45 0 3 1.69 3 3.5 0 1.41-.91 2.72-2 3.25V8.59c.58-.45 1-1.27 1-2.09C10 5.22 8.98 4 8 4H4c-.98 0-2 1.22-2 2.5S3 9 4 9zm9-3h-1v1h1c1 0 2 1.22 2 2.5S13.98 12 13 12H9c-.98 0-2-1.22-2-2.5 0-.83.42-1.64 1-2.09V6.25c-1.09.53-2 1.84-2 3.25C6 11.31 7.55 13 9 13h4c1.45 0 3-1.69 3-3.5S14.5 6 13 6z">
                  </path>
                </svg>
              </a>
              Scope Definition
            </h2>
            <p>
              The definition of a scope contains the following five attributes:
            </p>
            <table>
              <thead>
                <tr>
                  <th>
                    Attribute
                  </th>
                  <th>
                    Description
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td>
                    <code>
                      interfaces
                    </code>
                  </td>
                  <td>
                    A list of matcher which defines the required interfaces of the target method's class.
                  </td>
                </tr>
                <tr>
                  <td>
                    <code>
                      superclass
                    </code>
                  </td>
                  <td>
                    A matcher which defines the required superclass of the target method's class.
                  </td>
                </tr>
                <tr>
                  <td>
                    <code>
                      type
                    </code>
                  </td>
                  <td>
                    A matcher which defines the required type of the target method's class.
                  </td>
                </tr>
                <tr>
                  <td>
                    <code>
                      methods
                    </code>
                  </td>
                  <td>
                    A list of matchers which defines the target methods itself.
                  </td>
                </tr>
                <tr>
                  <td>
                    <code>
                      advanced
                    </code>
                  </td>
                  <td>
                    Advanced settings.
                  </td>
                </tr>
              </tbody>
            </table>
            <p>
              In order to determine which methods should be instrumented all classes are checked against the defined 
              <code>
                interface
              </code>
              , 
              <code>
                superclass
              </code>
              and 
              <code>
                type
              </code>
              matchers.
              If and only if a class matches on 
              <em>
                all
              </em>
              matchers, each of their methods is checked against the defined method matchers in order to determine the target methods.
              Thus, the process of determine whether the methods of a class 
              <code>
                X
              </code>
              should be instrumented can be represented as:
            </p>
            <ol>
              <li>
                Check if all interface matcher matches the interface of class 
                <code>
                  X
                </code>
                (if any is defined)
              </li>
              <li>
                Check if the superclass matcher matches any superclass of class 
                <code>
                  X
                </code>
                (if defined)
              </li>
              <li>
                Check if the type matcher matches the type of class 
                <code>
                  X
                </code>
                (if defined)
              </li>
              <li>
                For each method: check if the method is matching 
                <em>
                  any
                </em>
                of the defined method matchers (if defined)
              </li>
            </ol>
            <blockquote>
              <p>
                Keep in mind that all of the type matchers have to match whereas only one of the method matchers have to match!
              </p>
            </blockquote>
            <p>
              Based on the previous a scope definition looks like the following code-snippet:
            </p>
            <pre>
<code class="hljs css language-yaml">
<span class="hljs-comment">
# interfaces which have to be implemented
</span>
<span class="hljs-attr">
interfaces:
</span>
<span class="hljs-bullet">
-
</span>
<span class="hljs-comment">
# TYPE_MATCHER_A
</span>
<span class="hljs-bullet">
-
</span>
<span class="hljs-comment">
# TYPE_MATCHER_B
</span>
<span class="hljs-comment">
# the superclass which has to be extended
</span>
<span class="hljs-attr">
superclass:
</span>
<span class="hljs-comment">
# TYPE_MATCHER_C
</span>
<span class="hljs-comment">
# matcher describing the class' type
</span>
<span class="hljs-attr">
type:
</span>
<span class="hljs-comment">
# TYPE_MACHER_D
</span>
<span class="hljs-comment">
# the targeted method - each method which matches at least one of the defined matchers will be instrumented
</span>
<span class="hljs-attr">
methods:
</span>
<span class="hljs-bullet">
-
</span>
<span class="hljs-comment">
# METHOD_MATCHER_A
</span>
<span class="hljs-bullet">
-
</span>
<span class="hljs-comment">
# METHOD_MATCHER_B
</span>
</code>
</pre>
            <h2>
              <a class="anchor" aria-hidden="true" id="type-matcher">
              </a>
              <a href="#type-matcher" aria-hidden="true" class="hash-link">
                <svg class="hash-link-icon" aria-hidden="true" height="16" version="1.1" viewBox="0 0 16 16" width="16">
                  <path fill-rule="evenodd" d="M4 9h1v1H4c-1.5 0-3-1.69-3-3.5S2.55 3 4 3h4c1.45 0 3 1.69 3 3.5 0 1.41-.91 2.72-2 3.25V8.59c.58-.45 1-1.27 1-2.09C10 5.22 8.98 4 8 4H4c-.98 0-2 1.22-2 2.5S3 9 4 9zm9-3h-1v1h1c1 0 2 1.22 2 2.5S13.98 12 13 12H9c-.98 0-2-1.22-2-2.5 0-.83.42-1.64 1-2.09V6.25c-1.09.53-2 1.84-2 3.25C6 11.31 7.55 13 9 13h4c1.45 0 3-1.69 3-3.5S14.5 6 13 6z">
                  </path>
                </svg>
              </a>
              Type Matcher
            </h2>
            <p>
              As shown in the previous code-snippet, the scope definition contains multiple type matchers which are used to match interfaces, superclasses and class types itself.
            </p>
            <p>
              A type matcher consists of the following attributes:
            </p>
            <table>
              <thead>
                <tr>
                  <th>
                    Attribute
                  </th>
                  <th>
                    Default
                  </th>
                  <th>
                    Description
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td>
                    <code>
                      name
                    </code>
                  </td>
                  <td>
                    -
                  </td>
                  <td>
                    The name or pattern which is used to match against the fully qualified class or interface name.
                  </td>
                </tr>
                <tr>
                  <td>
                    <code>
                      matcher-mode
                    </code>
                  </td>
                  <td>
                    <code>
                      EQUALS_FULLY
                    </code>
                  </td>
                  <td>
                    The matching mode. Possible values: 
                    <code>
                      EQUALS_FULLY
                    </code>
                    , 
                    <code>
                      MATCHES
                    </code>
                    (see 
                    <a href="https://docs.oracle.com/javase/8/docs/api/java/lang/String.html#matches-java.lang.String">
                      String.match
                    </a>
                    ), 
                    <code>
                      STARTS_WITH
                    </code>
                    , 
                    <code>
                      STARTS_WITH_IGNORE_CASE
                    </code>
                    , 
                    <code>
                      CONTAINS
                    </code>
                    , 
                    <code>
                      CONTAINS_IGNORE_CASE
                    </code>
                    , 
                    <code>
                      ENDS_WITH
                    </code>
                    , 
                    <code>
                      ENDS_WITH_IGNORE_CASE
                    </code>
                  </td>
                </tr>
                <tr>
                  <td>
                    <code>
                      annotations
                    </code>
                  </td>
                  <td>
                    -
                  </td>
                  <td>
                    A list of matchers used for matching annotations. Each annotation matcher consists of a 
                    <code>
                      name
                    </code>
                    and 
                    <code>
                      matcher-mode
                    </code>
                    which are equivalent to the ones above.
                  </td>
                </tr>
              </tbody>
            </table>
            <p>
              The following example will match against a type which is exactly named 
              <code>
                java.util.AbstractList
              </code>
              and is annotated with the annotation 
              <code>
                any.Annotation
              </code>
              .
            </p>
            <pre>
<code class="hljs css language-yaml">
<span class="hljs-attr">
name:
</span>
<span class="hljs-string">
'java.util.AbstractList'
</span>
<span class="hljs-attr">
matcher-mode:
</span>
<span class="hljs-string">
EQUALS_FULLY
</span>
<span class="hljs-attr">
annotations:
</span>
<span class="hljs-bullet">
-
</span>
<span class="hljs-attr">
name:
</span>
<span class="hljs-string">
'any.Annotation'
</span>
<span class="hljs-attr">
matcher-mode:
</span>
<span class="hljs-string">
EQUALS_FULLY
</span>
</code>
</pre>
            <h2>
              <a class="anchor" aria-hidden="true" id="method-matcher">
              </a>
              <a href="#method-matcher" aria-hidden="true" class="hash-link">
                <svg class="hash-link-icon" aria-hidden="true" height="16" version="1.1" viewBox="0 0 16 16" width="16">
                  <path fill-rule="evenodd" d="M4 9h1v1H4c-1.5 0-3-1.69-3-3.5S2.55 3 4 3h4c1.45 0 3 1.69 3 3.5 0 1.41-.91 2.72-2 3.25V8.59c.58-.45 1-1.27 1-2.09C10 5.22 8.98 4 8 4H4c-.98 0-2 1.22-2 2.5S3 9 4 9zm9-3h-1v1h1c1 0 2 1.22 2 2.5S13.98 12 13 12H9c-.98 0-2-1.22-2-2.5 0-.83.42-1.64 1-2.09V6.25c-1.09.53-2 1.84-2 3.25C6 11.31 7.55 13 9 13h4c1.45 0 3-1.69 3-3.5S14.5 6 13 6z">
                  </path>
                </svg>
              </a>
              Method Matcher
            </h2>
            <p>
              The matcher used to determine whether a method is affected by a certain scope contains the 
              <em>
                same attributes as the previously described type matcher
              </em>
              but also contains additional ones.
            </p>
            <p>
              Besides 
              <code>
                name
              </code>
              , 
              <code>
                matcher-mode
              </code>
              and 
              <code>
                annotations
              </code>
              , the method matcher contains the following attributes:
            </p>
            <table>
              <thead>
                <tr>
                  <th>
                    Attribute
                  </th>
                  <th>
                    Default
                  </th>
                  <th>
                    Description
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td>
                    <code>
                      visibility
                    </code>
                  </td>
                  <td>
                    [PUBLIC, PROTECTED, PACKAGE, PRIVATE]
                  </td>
                  <td>
                    A list of visibility modifiers. The target method has to use one of the specified modifiers. Possible values: 
                    <code>
                      PUBLIC
                    </code>
                    , 
                    <code>
                      PROTECTED
                    </code>
                    , 
                    <code>
                      PACKAGE
                    </code>
                    , 
                    <code>
                      PRIVATE
                    </code>
                  </td>
                </tr>
                <tr>
                  <td>
                    <code>
                      arguments
                    </code>
                  </td>
                  <td>
                    -
                  </td>
                  <td>
                    A list of fully qualified class names representing the method's arguments.
                  </td>
                </tr>
                <tr>
                  <td>
                    <code>
                      is-synchronized
                    </code>
                  </td>
                  <td>
                    -
                  </td>
                  <td>
                    Specifies whether the target method is synchronized.
                  </td>
                </tr>
                <tr>
                  <td>
                    <code>
                      is-constructor
                    </code>
                  </td>
                  <td>
                    <code>
                      false
                    </code>
                  </td>
                  <td>
                    Specifies whether the target method is a constructor. If this value is 
                    <code>
                      true
                    </code>
                    , the 
                    <code>
                      name
                    </code>
                    and 
                    <code>
                      is-synchronized
                    </code>
                    attribute will 
                    <em>
                      not
                    </em>
                    be used!
                  </td>
                </tr>
              </tbody>
            </table>
            <p>
              The following example will match against all methods which are exactly named 
              <code>
                contains
              </code>
              , use the 
              <code>
                PUBLIC
              </code>
              visibility modifier, have exactly one argument which is a 
              <code>
                java.lang.Object
              </code>
              , are not synchronized and are annotated by the annotation 
              <code>
                any.Annotation
              </code>
              .
            </p>
            <pre>
<code class="hljs css language-yaml">
<span class="hljs-attr">
name:
</span>
<span class="hljs-string">
'contains'
</span>
<span class="hljs-attr">
matcher-mode:
</span>
<span class="hljs-string">
EQUALS_FULLY
</span>
<span class="hljs-attr">
visibility:
</span>
<span class="hljs-string">
[PUBLIC]
</span>
<span class="hljs-attr">
arguments:
</span>
<span class="hljs-string">
["java.lang.Object"]
</span>
<span class="hljs-attr">
is-synchronized:
</span>
<span class="hljs-literal">
false
</span>
<span class="hljs-attr">
annotations:
</span>
<span class="hljs-bullet">
-
</span>
<span class="hljs-attr">
name:
</span>
<span class="hljs-string">
'any.Annotation'
</span>
<span class="hljs-attr">
matcher-mode:
</span>
<span class="hljs-string">
EQUALS_FULLY
</span>
<span class="hljs-attr">
is-constructor:
</span>
<span class="hljs-literal">
false
</span>
</code>
</pre>
            <h2>
              <a class="anchor" aria-hidden="true" id="advanced-settings">
              </a>
              <a href="#advanced-settings" aria-hidden="true" class="hash-link">
                <svg class="hash-link-icon" aria-hidden="true" height="16" version="1.1" viewBox="0 0 16 16" width="16">
                  <path fill-rule="evenodd" d="M4 9h1v1H4c-1.5 0-3-1.69-3-3.5S2.55 3 4 3h4c1.45 0 3 1.69 3 3.5 0 1.41-.91 2.72-2 3.25V8.59c.58-.45 1-1.27 1-2.09C10 5.22 8.98 4 8 4H4c-.98 0-2 1.22-2 2.5S3 9 4 9zm9-3h-1v1h1c1 0 2 1.22 2 2.5S13.98 12 13 12H9c-.98 0-2-1.22-2-2.5 0-.83.42-1.64 1-2.09V6.25c-1.09.53-2 1.84-2 3.25C6 11.31 7.55 13 9 13h4c1.45 0 3-1.69 3-3.5S14.5 6 13 6z">
                  </path>
                </svg>
              </a>
              Advanced Settings
            </h2>
            <p>
              The scope definition's advanced settings contains currently the following two attributes:
            </p>
            <table>
              <thead>
                <tr>
                  <th>
                    Attribute
                  </th>
                  <th>
                    Default
                  </th>
                  <th>
                    Description
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td>
                    <code>
                      instrument-only-inherited-methods
                    </code>
                  </td>
                  <td>
                    false
                  </td>
                  <td>
                    If this value is 
                    <code>
                      true
                    </code>
                    , only methods will be instrumented which are inherited of a superclass or interface which were specified in the 
                    <code>
                      interfaces
                    </code>
                    or 
                    <code>
                      superclass
                    </code>
                    configuration.
                  </td>
                </tr>
                <tr>
                  <td>
                    <code>
                      disable-safety-mechanisms
                    </code>
                  </td>
                  <td>
                    false
                  </td>
                  <td>
                    By default, the agent will not allow scopes containing only "any-matcher" like 
                    <code>
                      MATCHES(*)
                    </code>
                    . If required, this safety feature can be disabled by setting this value to 
                    <code>
                      true
                    </code>
                    .
                  </td>
                </tr>
              </tbody>
            </table>
            <h2>
              <a class="anchor" aria-hidden="true" id="example-scope-definition">
              </a>
              <a href="#example-scope-definition" aria-hidden="true" class="hash-link">
                <svg class="hash-link-icon" aria-hidden="true" height="16" version="1.1" viewBox="0 0 16 16" width="16">
                  <path fill-rule="evenodd" d="M4 9h1v1H4c-1.5 0-3-1.69-3-3.5S2.55 3 4 3h4c1.45 0 3 1.69 3 3.5 0 1.41-.91 2.72-2 3.25V8.59c.58-.45 1-1.27 1-2.09C10 5.22 8.98 4 8 4H4c-.98 0-2 1.22-2 2.5S3 9 4 9zm9-3h-1v1h1c1 0 2 1.22 2 2.5S13.98 12 13 12H9c-.98 0-2-1.22-2-2.5 0-.83.42-1.64 1-2.09V6.25c-1.09.53-2 1.84-2 3.25C6 11.31 7.55 13 9 13h4c1.45 0 3-1.69 3-3.5S14.5 6 13 6z">
                  </path>
                </svg>
              </a>
              Example Scope Definition
            </h2>
            <p>
              The following code-snippet contains an example of a complete scope definitions.
              Note: the following configuration contains all possible attributes even though they are not all necessary!
            </p>
            <pre>
<code class="hljs css language-yaml">
<span class="hljs-attr">
inspectit:
</span>
<span class="hljs-attr">
instrumentation:
</span>
<span class="hljs-attr">
scopes:
</span>
<span class="hljs-comment">
# the id of the following defined scope element - this example scope targets the ArrayList's contains method
</span>
<span class="hljs-attr">
's_example_list_scope':
</span>
<span class="hljs-comment">
# interfaces which have to be implemented
</span>
<span class="hljs-attr">
interfaces:
</span>
<span class="hljs-bullet">
-
</span>
<span class="hljs-attr">
name:
</span>
<span class="hljs-string">
'java.util.List'
</span>
<span class="hljs-attr">
matcher-mode:
</span>
<span class="hljs-string">
EQUALS_FULLY
</span>
<span class="hljs-attr">
annotations:
</span>
<span class="hljs-bullet">
-
</span>
<span class="hljs-attr">
name:
</span>
<span class="hljs-string">
'any.Annotation'
</span>
<span class="hljs-attr">
matcher-mode:
</span>
<span class="hljs-string">
EQUALS_FULLY
</span>
<span class="hljs-comment">
# the superclass which has to be extended
</span>
<span class="hljs-attr">
superclass:
</span>
<span class="hljs-attr">
name:
</span>
<span class="hljs-string">
'java.util.AbstractList'
</span>
<span class="hljs-attr">
matcher-mode:
</span>
<span class="hljs-string">
EQUALS_FULLY
</span>
<span class="hljs-attr">
annotations:
</span>
<span class="hljs-bullet">
-
</span>
<span class="hljs-attr">
name:
</span>
<span class="hljs-string">
'any.Annotation'
</span>
<span class="hljs-attr">
matcher-mode:
</span>
<span class="hljs-string">
EQUALS_FULLY
</span>
<span class="hljs-comment">
# matcher describing the class' name (full qualified)
</span>
<span class="hljs-attr">
type:
</span>
<span class="hljs-attr">
name:
</span>
<span class="hljs-string">
'ArrayList'
</span>
<span class="hljs-attr">
matcher-mode:
</span>
<span class="hljs-string">
ENDS_WITH
</span>
<span class="hljs-attr">
annotations:
</span>
<span class="hljs-bullet">
-
</span>
<span class="hljs-attr">
name:
</span>
<span class="hljs-string">
'any.Annotation'
</span>
<span class="hljs-attr">
matcher-mode:
</span>
<span class="hljs-string">
EQUALS_FULLY
</span>
<span class="hljs-comment">
# the targeted method - each method which matches at least one of the defined matchers will be instrumented
</span>
<span class="hljs-attr">
methods:
</span>
<span class="hljs-bullet">
-
</span>
<span class="hljs-attr">
name:
</span>
<span class="hljs-string">
'contains'
</span>
<span class="hljs-attr">
matcher-mode:
</span>
<span class="hljs-string">
EQUALS_FULLY
</span>
<span class="hljs-attr">
visibility:
</span>
<span class="hljs-string">
[PUBLIC]
</span>
<span class="hljs-attr">
arguments:
</span>
<span class="hljs-string">
["java.lang.Object"]
</span>
<span class="hljs-attr">
is-synchronized:
</span>
<span class="hljs-literal">
false
</span>
<span class="hljs-attr">
annotations:
</span>
<span class="hljs-bullet">
-
</span>
<span class="hljs-attr">
name:
</span>
<span class="hljs-string">
'any.Annotation'
</span>
<span class="hljs-attr">
matcher-mode:
</span>
<span class="hljs-string">
EQUALS_FULLY
</span>
<span class="hljs-bullet">
-
</span>
<span class="hljs-attr">
is-constructor:
</span>
<span class="hljs-literal">
true
</span>
<span class="hljs-attr">
visibility:
</span>
<span class="hljs-string">
[PUBLIC]
</span>
<span class="hljs-attr">
arguments:
</span>
<span class="hljs-string">
[]
</span>
<span class="hljs-attr">
annotations:
</span>
<span class="hljs-bullet">
-
</span>
<span class="hljs-attr">
name:
</span>
<span class="hljs-string">
'any.Annotation'
</span>
<span class="hljs-attr">
matcher-mode:
</span>
<span class="hljs-string">
EQUALS_FULLY
</span>
<span class="hljs-comment">
# advances settings which can be used to specify and narrow the instrumentation
</span>
<span class="hljs-attr">
advanced:
</span>
<span class="hljs-attr">
instrument-only-inherited-methods:
</span>
<span class="hljs-literal">
false
</span>
<span class="hljs-attr">
disable-safety-mechanisms:
</span>
<span class="hljs-literal">
false
</span>
</code>
</pre>
          </span>
        </div>
      </article>
    </div>
    <div class="docLastUpdate">
      <em>
        Last updated on 2020-5-5 by Marius Brill
      </em>
    </div>
    <div class="docs-prevnext">
      <a class="docs-prev button" href="/inspectit-ocelot/docs/instrumentation/instrumentation">
        <span class="arrow-prev">
          ← 
        </span>
        <span>
          Instrumentation
        </span>
      </a>
      <a class="docs-next button" href="/inspectit-ocelot/docs/instrumentation/rules">
        <span>
          Rules
        </span>
        <span class="arrow-next">
          →
        </span>
      </a>
    </div>
  </div>
</div>
</div>
)
}
