# HowTo Front End Testing

This section gives a short introduction of how to write front end tests for Configuration Server UI and what to cover. 

## Introduction:
These testing practices were thought of with regard to the current architecture and project structure. Also, this can 
and will still grow and change over time, this is the first step into introducing front end tests. We distinguish 
between render and functionality tests. Render tests will make use of snapshots to check whether the DOM changed
unknowingly.
Functionality tests are covering the functionality of a component. The focus here is to 
imitate user (inter-)actions and assert the expected state.

## Render Tests
A render test takes a component, checks its DOM (Document Object Model) and compares it to a snapshot file that is stored from an 
earlier point in time. When changes are detected, the test will fail and thereby indicate that potentially
unwanted changes happened.

It is recommended to test, at the very least, smaller components via snapshots. As already mentioned, snapshot tests will
fail when a change in the DOM is found, so testing big components will result in them failing often 
(whenever a change in any subcomponent happens) and will also result in very big and unreadable snapshot files. Bigger components 
will have a shallow render test that only render a component one level deep and disregarding child components' contents.

## Functionality Tests:
It makes sense to write tests that verify the functionalities of every component implemented. With a functionality test, 
we check for the (basic) functionality of a component. This can be a disabled button or an error message of a validation
when a certain text-field is empty.

This type of test usually covers the user interactions and verifies their functionality. Checking for the presence of 
certain elements is not covered here. We do not distinguish whether the component is very small or has many children, but
it is highly advised to isolate the tests for every scenario of interaction and not test many scenarios in one test.
At a later point in time, E2E Tests might be introduced as well. Those should be treated with caution however, since they
are very expensive compared to basic component (functionality) tests.

## Helpful links:

https://testing-library.com/docs/

https://kentcdodds.com/