import ace from 'ace-builds/src-noconflict/ace';
import 'ace-builds/src-noconflict/mode-java';
var oop = ace.require("ace/lib/oop");
const TextHighlightRules = ace.require("ace/mode/text_highlight_rules").TextHighlightRules;
const JavaHighlightRules = ace.require("ace/mode/java_highlight_rules").JavaHighlightRules ;

var InspectitOcelotHighlightRules = function() {

    const commentRule = {
        token : "comment",
        regex : "#.*$"
    }

    const defaultRule = {
        defaultToken : "text"
    }

    // For the limited support of JSON objects within the YAML, opening braces need to be allowed
    // and detected as such, before strings are tested for matching with expected YAML keys in each state.
    const jsonStartRule = {
        token : "paren.lparen",
        regex : "{"
    }

    function getMultilineStartRule(start_state, pre_state){
        return {
            token: "string.mlStart", // multi line string start
            regex: /[|>][-+\d]*(?:$|\s+(?:$|#))/,
            onMatch: function (val, state, stack, line) {
                line = line.replace(/ #.*/, "");
                var indent = /^ *((:\s*)?-(\s*[^|>])?)?/.exec(line)[0]
                    .replace(/\S\s*$/, "").length;
                var indentationIndicator = parseInt(/\d+[\s+-]*$/.exec(line));

                if (indentationIndicator) {
                    indent += indentationIndicator - 1;
                    this.next = start_state;
                } else {
                    this.next = pre_state;
                }
                if (!stack.length) {
                    stack.push(this.next);
                    stack.push(indent);
                    stack.push(state);
                } else {
                    stack[0] = this.next;
                    stack[1] = indent;
                    stack[2] = state;
                }
                return this.token;
            }
        }
    }

    const textRules = [
        {
            token: "constant",
            regex: "!![\\w//]+"
        }, {
            token: "constant.language",
            regex: "[&\\*][a-zA-Z0-9-_]+"
        }, {
            token : "keyword.operator",
            regex : "<<\\w*:\\w*"
        }, {
            token : "keyword.operator",
            regex : "-\\s*(?=[{])"
        }, {
            token : "string", // single line
            regex : '["](?:(?:\\\\.)|(?:[^"\\\\]))*?["]'
        }, {
            token : "string", // single quoted string
            regex : "['](?:(?:\\\\.)|(?:[^'\\\\]))*?[']"
        }, getMultilineStartRule("mlString", "mlStringPre"),
        {
            token : "constant.numeric", // float
            regex : /(\b|[+\-\.])[\d_]+(?:(?:\.[\d_]*)?(?:[eE][+\-]?[\d_]+)?)(?=[^\d-\w]|$)/
        }, {
            token : "constant.numeric", // other number
            regex : /[+\-]?\.inf\b|NaN\b|0x[\dA-Fa-f_]+|0b[10_]+/
        }, {
            token : "constant.language.boolean",
            regex : "\\b(?:true|false|TRUE|FALSE|True|False|yes|no)\\b"
        }, {
            token : "paren.lparen",
            regex : "[[({]"
        }, {
            token : "paren.rparen",
            regex : "[\\])}]"
        }
    ]

    const yamlRules = textRules.concat(
        [
            /*needed because the meta.tag rule was removed from the standard yaml highlight rules to be able to 
            detect wrong tags, but there are no wrong tags possible in states that use these rules.*/
            {
                token: ["meta.tag", "keyword"],
                regex: /(\w+?)(\s*:(?:\s+|$))/
            },

            /*needed because standard list rule expects the list to be matched with the
            beginning of the line and the whitespaces between, but those are already handled
            and tokenized as indents, so we need a list rule that works without the beginning
            of the line.*/
            {
                token : "list.markup",
                regex : /\s*[\-?](?:$|\s)/
            }
        ]
    )

    const javaRules = [
        //Java code is always within Strings (it really just is strings in the background too), so it can be both a 
        // single line in quotes or multiple lines just like Strings can be within a YAML document.
        {
            token: "keyword.java-sl-start",
            regex: /([:](?=(?:^\|'|"|""")*$))/,
            push: "java-singleline-start"
        }, {
            token: "text.java-sl-start",
            regex: /('|"|""")/,
            push: "java-singleline-start"
        }, getMultilineStartRule("java-multi-line-start", "java-multi-line-pre")
    ]

    const keywordRule = {
        token: "keyword",
        regex: /:\s*/
    }

    const invalidRule = {
        token: "invalid.illegal",
        regex: /.*/,
        onMatch: function(val, state, stack) {
            return `invalid.deprecated.${state}`;
        }
    }

    // rulesToGenerate is a map whose contents determine what the highlighting rules need to be. The contents are
    // generated in Java within the confg-server and retrieved over the confg-server's REST API
    const rulesToGenerate = new Map(Object.entries(
        {"start":{"object-attributes":{"inspectit":{"object-attributes":{"tracing":{"object-attributes":{"propagation-format":{"enum-values":["B3","TRACE_CONTEXT","DATADOG"],"type":"enum"},"log-correlation":{"object-attributes":{"trace-id-mdc-injection":{"object-attributes":{"log4j2-enabled":{"type":"text"},"slf4j-enabled":{"type":"text"},"jboss-logmanager-enabled":{"type":"text"},"enabled":{"type":"text"},"key":{"type":"text"},"log4j1-enabled":{"type":"text"}},"type":"object"},"trace-id-auto-injection":{"object-attributes":{"prefix":{"type":"text"},"suffix":{"type":"text"},"enabled":{"type":"text"}},"type":"object"}},"type":"object"},"auto-tracing":{"object-attributes":{"shutdown-delay":{"type":"text"},"frequency":{"type":"text"}},"type":"object"},"add-common-tags":{"enum-values":["NEVER","ON_GLOBAL_ROOT","ON_LOCAL_ROOT","ALWAYS"],"type":"enum"},"sample-probability":{"type":"text"},"enabled":{"type":"text"}},"type":"object"},"plugins":{"object-attributes":{"path":{"type":"text"}},"type":"object"},"privacy":{"object-attributes":{"obfuscation":{"object-attributes":{"patterns":{"list-contents":{"case-insensitive":{"type":"text"},"pattern":{"type":"text"},"check-key":{"type":"text"},"check-data":{"type":"text"},"replace-regex":{"type":"text"}},"list-content-type":"object","type":"list"},"enabled":{"type":"text"}},"type":"object"}},"type":"object"},"env":{"object-attributes":{"hostname":{"type":"text"},"agent-version":{"type":"text"},"java-version":{"type":"text"},"pid":{"type":"text"},"agent-dir":{"type":"text"}},"type":"object"},"thread-pool-size":{"type":"text"},"tags":{"object-attributes":{"extra":{"type":"map","map-content-type":"text"},"providers":{"object-attributes":{"environment":{"object-attributes":{"resolve-host-address":{"type":"text"},"resolve-host-name":{"type":"text"},"enabled":{"type":"text"}},"type":"object"}},"type":"object"}},"type":"object"},"agent-commands":{"object-attributes":{"live-mode-duration":{"type":"text"},"derive-from-http-config-url":{"type":"text"},"live-socket-timeout":{"type":"text"},"socket-timeout":{"type":"text"},"agent-command-path":{"type":"text"},"enabled":{"type":"text"},"url":{"type":"text"},"polling-interval":{"type":"text"}},"type":"object"},"service-name":{"type":"text"},"publish-open-census-to-bootstrap":{"type":"text"},"logging":{"object-attributes":{"console":{"object-attributes":{"pattern":{"type":"text"},"enabled":{"type":"text"}},"type":"object"},"trace":{"type":"text"},"config-file":{"type":"text"},"debug":{"type":"text"},"file":{"object-attributes":{"path":{"type":"text"},"pattern":{"type":"text"},"enabled":{"type":"text"},"include-service-name":{"type":"text"}},"type":"object"}},"type":"object"},"metrics":{"object-attributes":{"disk":{"object-attributes":{"enabled":{"type":"map","map-content-type":"text"},"frequency":{"type":"text"}},"type":"object"},"memory":{"object-attributes":{"enabled":{"type":"map","map-content-type":"text"},"frequency":{"type":"text"}},"type":"object"},"jmx":{"object-attributes":{"object-names":{"type":"map","map-content-type":"text"},"force-platform-server":{"type":"text"},"lower-case-metric-name":{"type":"text"},"enabled":{"type":"text"},"frequency":{"type":"text"}},"type":"object"},"classloader":{"object-attributes":{"enabled":{"type":"map","map-content-type":"text"},"frequency":{"type":"text"}},"type":"object"},"threads":{"object-attributes":{"enabled":{"type":"map","map-content-type":"text"},"frequency":{"type":"text"}},"type":"object"},"gc":{"object-attributes":{"enabled":{"type":"map","map-content-type":"text"}},"type":"object"},"definitions":{"map-contents":{"unit":{"type":"text"},"description":{"type":"text"},"type":{"enum-values":["LONG","DOUBLE"],"type":"enum"},"enabled":{"type":"text"},"views":{"map-contents":{"quantiles":{"list-contents":{"value":{"type":"text"}},"list-content-type":"object","type":"list"},"drop-upper":{"type":"text"},"with-common-tags":{"type":"text"},"drop-lower":{"type":"text"},"description":{"type":"text"},"aggregation":{"enum-values":["LAST_VALUE","SUM","COUNT","QUANTILES","SMOOTHED_AVERAGE","HISTOGRAM"],"type":"enum"},"enabled":{"type":"text"},"time-window":{"type":"text"},"bucket-boundaries":{"list-contents":{"value":{"type":"text"}},"list-content-type":"object","type":"list"},"max-buffered-points":{"type":"text"},"tags":{"type":"map","map-content-type":"text"}},"type":"map","map-content-type":"object"}},"type":"map","map-content-type":"object"},"processor":{"object-attributes":{"enabled":{"type":"map","map-content-type":"text"},"frequency":{"type":"text"}},"type":"object"},"enabled":{"type":"text"},"frequency":{"type":"text"}},"type":"object"},"instrumentation":{"object-attributes":{"special":{"object-attributes":{"scheduled-executor-context-propagation":{"type":"text"},"thread-start-context-propagation":{"type":"text"},"class-loader-delegation":{"type":"text"},"executor-context-propagation":{"type":"text"}},"type":"object"},"internal":{"object-attributes":{"class-retransform-batch-size":{"type":"text"},"new-class-discovery-interval":{"type":"text"},"num-class-discovery-trials":{"type":"text"},"use-inspectit-protection-domain":{"type":"text"},"class-configuration-check-batch-size":{"type":"text"},"recycling-old-action-classes":{"type":"text"},"inter-batch-delay":{"type":"text"}},"type":"object"},"data":{"map-contents":{"is-tag":{"type":"text"},"up-propagation":{"enum-values":["NONE","JVM_LOCAL","GLOBAL"],"type":"enum"},"down-propagation":{"enum-values":["NONE","JVM_LOCAL","GLOBAL"],"type":"enum"}},"type":"map","map-content-type":"object"},"rules":{"map-contents":{"post-exit":{"map-contents":{"constant-input":{"type":"map","map-content-type":"yaml"},"action":{"type":"text"},"only-if-null":{"type":"text"},"only-if-not-null":{"type":"text"},"only-if-true":{"type":"text"},"only-if-false":{"type":"text"},"data-input":{"type":"map","map-content-type":"text"},"order":{"object-attributes":{"reads":{"type":"map","map-content-type":"text"},"reads-before-written":{"type":"map","map-content-type":"text"},"writes":{"type":"map","map-content-type":"text"}},"type":"object"}},"type":"map","map-content-type":"object"},"include":{"type":"map","map-content-type":"text"},"pre-entry":{"map-contents":{"constant-input":{"type":"map","map-content-type":"yaml"},"action":{"type":"text"},"only-if-null":{"type":"text"},"only-if-not-null":{"type":"text"},"only-if-true":{"type":"text"},"only-if-false":{"type":"text"},"data-input":{"type":"map","map-content-type":"text"},"order":{"object-attributes":{"reads":{"type":"map","map-content-type":"text"},"reads-before-written":{"type":"map","map-content-type":"text"},"writes":{"type":"map","map-content-type":"text"}},"type":"object"}},"type":"map","map-content-type":"object"},"entry":{"map-contents":{"constant-input":{"type":"map","map-content-type":"yaml"},"action":{"type":"text"},"only-if-null":{"type":"text"},"only-if-not-null":{"type":"text"},"only-if-true":{"type":"text"},"only-if-false":{"type":"text"},"data-input":{"type":"map","map-content-type":"text"},"order":{"object-attributes":{"reads":{"type":"map","map-content-type":"text"},"reads-before-written":{"type":"map","map-content-type":"text"},"writes":{"type":"map","map-content-type":"text"}},"type":"object"}},"type":"map","map-content-type":"object"},"post-entry":{"map-contents":{"constant-input":{"type":"map","map-content-type":"yaml"},"action":{"type":"text"},"only-if-null":{"type":"text"},"only-if-not-null":{"type":"text"},"only-if-true":{"type":"text"},"only-if-false":{"type":"text"},"data-input":{"type":"map","map-content-type":"text"},"order":{"object-attributes":{"reads":{"type":"map","map-content-type":"text"},"reads-before-written":{"type":"map","map-content-type":"text"},"writes":{"type":"map","map-content-type":"text"}},"type":"object"}},"type":"map","map-content-type":"object"},"exit":{"map-contents":{"constant-input":{"type":"map","map-content-type":"yaml"},"action":{"type":"text"},"only-if-null":{"type":"text"},"only-if-not-null":{"type":"text"},"only-if-true":{"type":"text"},"only-if-false":{"type":"text"},"data-input":{"type":"map","map-content-type":"text"},"order":{"object-attributes":{"reads":{"type":"map","map-content-type":"text"},"reads-before-written":{"type":"map","map-content-type":"text"},"writes":{"type":"map","map-content-type":"text"}},"type":"object"}},"type":"map","map-content-type":"object"},"tracing":{"object-attributes":{"continue-span":{"type":"text"},"end-span":{"type":"text"},"store-span":{"type":"text"},"kind":{"type":"text"},"start-span-conditions":{"object-attributes":{"only-if-null":{"type":"text"},"only-if-not-null":{"type":"text"},"only-if-true":{"type":"text"},"only-if-false":{"type":"text"}},"type":"object"},"attribute-conditions":{"object-attributes":{"only-if-null":{"type":"text"},"only-if-not-null":{"type":"text"},"only-if-true":{"type":"text"},"only-if-false":{"type":"text"}},"type":"object"},"error-status":{"type":"text"},"continue-span-conditions":{"object-attributes":{"only-if-null":{"type":"text"},"only-if-not-null":{"type":"text"},"only-if-true":{"type":"text"},"only-if-false":{"type":"text"}},"type":"object"},"start-span":{"type":"text"},"name":{"type":"text"},"end-span-conditions":{"object-attributes":{"only-if-null":{"type":"text"},"only-if-not-null":{"type":"text"},"only-if-true":{"type":"text"},"only-if-false":{"type":"text"}},"type":"object"},"auto-tracing":{"type":"text"},"attributes":{"type":"map","map-content-type":"text"},"sample-probability":{"type":"text"}},"type":"object"},"pre-exit":{"map-contents":{"constant-input":{"type":"map","map-content-type":"yaml"},"action":{"type":"text"},"only-if-null":{"type":"text"},"only-if-not-null":{"type":"text"},"only-if-true":{"type":"text"},"only-if-false":{"type":"text"},"data-input":{"type":"map","map-content-type":"text"},"order":{"object-attributes":{"reads":{"type":"map","map-content-type":"text"},"reads-before-written":{"type":"map","map-content-type":"text"},"writes":{"type":"map","map-content-type":"text"}},"type":"object"}},"type":"map","map-content-type":"object"},"scopes":{"type":"map","map-content-type":"text"},"metrics":{"map-contents":{"metric":{"type":"text"},"data-tags":{"type":"map","map-content-type":"text"},"constant-tags":{"type":"map","map-content-type":"text"},"value":{"type":"text"}},"type":"map","map-content-type":"object"},"enabled":{"type":"text"}},"type":"map","map-content-type":"object"},"scopes":{"map-contents":{"interfaces":{"list-contents":{"matcher-mode":{"enum-values":["EQUALS_FULLY","EQUALS_FULLY_IGNORE_CASE","STARTS_WITH","STARTS_WITH_IGNORE_CASE","ENDS_WITH","ENDS_WITH_IGNORE_CASE","CONTAINS","CONTAINS_IGNORE_CASE","MATCHES","NOT_EQUALS_FULLY","NOT_EQUALS_FULLY_IGNORE_CASE"],"type":"enum"},"name":{"type":"text"},"annotations":{"list-contents":{"matcher-mode":{"enum-values":["EQUALS_FULLY","EQUALS_FULLY_IGNORE_CASE","STARTS_WITH","STARTS_WITH_IGNORE_CASE","ENDS_WITH","ENDS_WITH_IGNORE_CASE","CONTAINS","CONTAINS_IGNORE_CASE","MATCHES","NOT_EQUALS_FULLY","NOT_EQUALS_FULLY_IGNORE_CASE"],"type":"enum"},"name":{"type":"text"}},"list-content-type":"object","type":"list"}},"list-content-type":"object","type":"list"},"advanced":{"object-attributes":{"disable-safety-mechanisms":{"type":"text"},"instrument-only-inherited-methods":{"type":"text"}},"type":"object"},"superclass":{"object-attributes":{"matcher-mode":{"enum-values":["EQUALS_FULLY","EQUALS_FULLY_IGNORE_CASE","STARTS_WITH","STARTS_WITH_IGNORE_CASE","ENDS_WITH","ENDS_WITH_IGNORE_CASE","CONTAINS","CONTAINS_IGNORE_CASE","MATCHES","NOT_EQUALS_FULLY","NOT_EQUALS_FULLY_IGNORE_CASE"],"type":"enum"},"name":{"type":"text"},"annotations":{"list-contents":{"matcher-mode":{"enum-values":["EQUALS_FULLY","EQUALS_FULLY_IGNORE_CASE","STARTS_WITH","STARTS_WITH_IGNORE_CASE","ENDS_WITH","ENDS_WITH_IGNORE_CASE","CONTAINS","CONTAINS_IGNORE_CASE","MATCHES","NOT_EQUALS_FULLY","NOT_EQUALS_FULLY_IGNORE_CASE"],"type":"enum"},"name":{"type":"text"}},"list-content-type":"object","type":"list"}},"type":"object"},"methods":{"list-contents":{"matcher-mode":{"enum-values":["EQUALS_FULLY","EQUALS_FULLY_IGNORE_CASE","STARTS_WITH","STARTS_WITH_IGNORE_CASE","ENDS_WITH","ENDS_WITH_IGNORE_CASE","CONTAINS","CONTAINS_IGNORE_CASE","MATCHES","NOT_EQUALS_FULLY","NOT_EQUALS_FULLY_IGNORE_CASE"],"type":"enum"},"visibility":{"list-contents":{"name":{"type":"text"},"ordinal":{"type":"text"}},"list-content-type":"object","type":"list"},"is-constructor":{"type":"text"},"name":{"type":"text"},"annotations":{"list-contents":{"matcher-mode":{"enum-values":["EQUALS_FULLY","EQUALS_FULLY_IGNORE_CASE","STARTS_WITH","STARTS_WITH_IGNORE_CASE","ENDS_WITH","ENDS_WITH_IGNORE_CASE","CONTAINS","CONTAINS_IGNORE_CASE","MATCHES","NOT_EQUALS_FULLY","NOT_EQUALS_FULLY_IGNORE_CASE"],"type":"enum"},"name":{"type":"text"}},"list-content-type":"object","type":"list"},"is-synchronized":{"type":"text"},"arguments":{"list-content-type":"text","type":"list"}},"list-content-type":"object","type":"list"},"exclude":{"type":"map","map-content-type":"text"},"type":{"object-attributes":{"matcher-mode":{"enum-values":["EQUALS_FULLY","EQUALS_FULLY_IGNORE_CASE","STARTS_WITH","STARTS_WITH_IGNORE_CASE","ENDS_WITH","ENDS_WITH_IGNORE_CASE","CONTAINS","CONTAINS_IGNORE_CASE","MATCHES","NOT_EQUALS_FULLY","NOT_EQUALS_FULLY_IGNORE_CASE"],"type":"enum"},"name":{"type":"text"},"annotations":{"list-contents":{"matcher-mode":{"enum-values":["EQUALS_FULLY","EQUALS_FULLY_IGNORE_CASE","STARTS_WITH","STARTS_WITH_IGNORE_CASE","ENDS_WITH","ENDS_WITH_IGNORE_CASE","CONTAINS","CONTAINS_IGNORE_CASE","MATCHES","NOT_EQUALS_FULLY","NOT_EQUALS_FULLY_IGNORE_CASE"],"type":"enum"},"name":{"type":"text"}},"list-content-type":"object","type":"list"}},"type":"object"}},"type":"map","map-content-type":"object"},"actions":{"map-contents":{"input":{"type":"map","map-content-type":"text"},"imports":{"list-content-type":"text","type":"list"},"is-void":{"type":"text"},"value-body":{"type":"java"},"value":{"type":"java"}},"type":"map","map-content-type":"object"},"exclude-lambdas":{"type":"text"},"enabled":{"type":"text"},"ignored-bootstrap-packages":{"type":"map","map-content-type":"text"},"ignored-packages":{"type":"map","map-content-type":"text"}},"type":"object"},"config":{"object-attributes":{"file-based":{"object-attributes":{"path":{"type":"text"},"watch":{"type":"text"},"enabled":{"type":"text"},"frequency":{"type":"text"}},"type":"object"},"http":{"object-attributes":{"connection-timeout":{"type":"text"},"persistence-file":{"type":"text"},"attributes":{"type":"map","map-content-type":"text"},"socket-timeout":{"type":"text"},"enabled":{"type":"text"},"url":{"type":"text"},"frequency":{"type":"text"}},"type":"object"}},"type":"object"},"self-monitoring":{"object-attributes":{"action-metrics":{"object-attributes":{"enabled":{"type":"text"}},"type":"object"},"enabled":{"type":"text"}},"type":"object"},"exporters":{"object-attributes":{"tracing":{"object-attributes":{"zipkin":{"object-attributes":{"service-name":{"type":"text"},"enabled":{"type":"text"},"url":{"type":"text"}},"type":"object"},"jaeger":{"object-attributes":{"service-name":{"type":"text"},"enabled":{"type":"text"},"url":{"type":"text"},"grpc":{"type":"text"}},"type":"object"},"open-census-agent":{"object-attributes":{"use-insecure":{"type":"text"},"service-name":{"type":"text"},"address":{"type":"text"},"reconnection-period":{"type":"text"},"enabled":{"type":"text"}},"type":"object"}},"type":"object"},"metrics":{"object-attributes":{"influx":{"object-attributes":{"database":{"type":"text"},"password":{"type":"text"},"create-database":{"type":"text"},"export-interval":{"type":"text"},"buffer-size":{"type":"text"},"counters-as-differences":{"type":"text"},"user":{"type":"text"},"enabled":{"type":"text"},"url":{"type":"text"},"retention-policy":{"type":"text"}},"type":"object"},"prometheus":{"object-attributes":{"port":{"type":"text"},"host":{"type":"text"},"enabled":{"type":"text"}},"type":"object"},"open-census-agent":{"object-attributes":{"use-insecure":{"type":"text"},"service-name":{"type":"text"},"address":{"type":"text"},"export-interval":{"type":"text"},"reconnection-period":{"type":"text"},"enabled":{"type":"text"}},"type":"object"}},"type":"object"}},"type":"object"}},"type":"object"}},"type":"object"}}
    ));


    /*
        Evaluates for a given indent, whether it is an error in general, it is correct for the current state, or
        it is generally correct but for a different state.
     */
    function evaluateIndent(val, state_name){

        let expected_indent = states.get(state_name).get("indent");
        let content_type = states.get(state_name).get("content-type");

        if ((val.length % 2 !== 0) || ((val.length > expected_indent) && (content_type !== "yaml"))){
            return [state_name, `invalid.illegal.indentationError`];
        } else if(val.length < expected_indent){
            let parent_states = states.get(state_name).get("parents");
            let states_up = (expected_indent - val.length) / 2;
            let new_state = parent_states[parent_states.length - states_up];
            return [new_state, undefined];
        } else {
            return [state_name, undefined];
        }

    }

    /*
        The indentRules make it so that whenever an indent is encountered, the highlighter checks if the indent fits
        with its current state or if it needs to change its state.
    */
    const indentRules = [
        // For empty lines nothing happens
        {
            token : "indent",
            regex : /^ *$/,
            onMatch: function(val, state, stack) {
                return `${state}-indent`;
            }
        },
        // For lines with content, the indent is evaluated and acted upon accordingly, i.e. staying in the current
        // state, switching back to a parent state or marking it as an error and switching to the indentationError-state.
        {
            token : "indent",
            regex : /^ */,
            onMatch: function(val, state, stack) {
                let result = evaluateIndent(val, state);
                if(result[1] !== undefined){
                    this.next = "indentationError";
                    stack.unshift(this.next, state);
                    return result[1];
                } else {
                    //console.log(result[0]);
                    this.next = result[0];
                    return `${this.next}-indent`;
                }
            }
        }]

    let allRules = new Map();


    /* 
    Adds a new state to the states-Map. This is executed if the highlighter sees a key that leads to entering a new state
    at a different indentation level. The states map is used to determine which state to enter if later down the line
    the indentation changes to something lower than the indentation of the current state of the highlighter. For this
    it contains info about the parent-states and the expected indentation of the current state.
    
    E. g. if the highlighter is in a current state and enters a new line, it looks whether the indent of that line
    is the one that's expected for this current state. If yes, it stays in it. If it's lower however it instead looks 
    at how much lower it is and then looks through the state's parents to choose the correct new state for this indentation.
     */
    function addToStatesMap(state_name, parent_name, content_type, nested_level){
        let parent_parents = states.get(parent_name).get("parents");
        let all_parents = parent_parents.concat([parent_name]);
        let state_entry = new Map();
        state_entry.set("indent", nested_level * 2);
        state_entry.set("parents", all_parents);
        state_entry.set("content-type", content_type);
        states.set(state_name, state_entry);
    }

    const states = new Map();
    const start_entry = new Map();
    start_entry.set("indent", 0);
    start_entry.set("parents", []);
    states.set("start", start_entry);

    /*
    Generates rules for the syntax highlighter based on the values in the given Map.
    Recursively calls itself for nested entries in the Map.
     */
    function mapToRules(map, parent_name, nested_level){

        // The given map contains as keys the possible values for keys in the YAML at the current state.
        // This for loop iterates through them.
        for(let key of map.keys()){

            // To avoid name-collisions all the names for states except for the "start"-state are created by
            // combining the parent's key and the current key
            let current_state_name;
            if(key==="start"){
                current_state_name = key;
            } else {
                current_state_name = `${key}-${parent_name}`;
            }

            // Rules for highlighting are kept in a list per state, this list for the state corresponding to the
            // current key is created here.

            // The list already contains two rules that are needed for every key and are at the correct position if
            // added now (the order of rules matters for evaluation, if a string matches a regex in a previous rule,
            // later rules are not evaluated anymore.
            let rules_for_current_key = [commentRule, defaultRule, keywordRule, jsonStartRule]

            // The info on what values should be behind a key, is within a nested map. This map is retrieved here.
            let inner_map = new Map(Object.entries(map.get(key)));

            // The type field determines what type the value has.
            let type = inner_map.get("type");

            if(type === "object"){

                rules_for_current_key = mapToRulesTypeObject(inner_map, rules_for_current_key, current_state_name, nested_level);

            } else if (type === "map"){

                rules_for_current_key = mapToRulesTypeMap(inner_map, rules_for_current_key, current_state_name, nested_level);

            } else if (type === "list"){

                rules_for_current_key = mapToRulesTypeList(inner_map, rules_for_current_key, current_state_name, nested_level);

            } else if(type==="java"){

                rules_for_current_key = rulesForJava(rules_for_current_key);

            } else if(type === "enum"){

                // the possible values for an enum are behind the key "enum-values" in the inner_map
                let enum_values = inner_map.get("enum-values");
                rules_for_current_key = rulesForEnum(rules_for_current_key, current_state_name, enum_values)

            } else if(type === "yaml") {

                rules_for_current_key = rulesForYaml(rules_for_current_key, current_state_name);

            } else if(type === "text"){
                rules_for_current_key = rules_for_current_key.concat(textRules);
            }
            if(key !== "start"){
                
                // start does not need them because in the start state the indent is always zero and it 
                // would lead to an infinite loop if you added them
                rules_for_current_key = indentRules.concat(rules_for_current_key);
            }
            // finally the new rules are added to the allRules map
            allRules.set(current_state_name, rules_for_current_key);
        }
    }

    /*
        Generates and returns the rules for states with InspectitConfig-specific objects
     */
    function rulesForObject(attributes_map, current_rules, current_state_name, nested_level){

        // If the value is an InspectitConfig-specific Object, each of that Object's attributes needs a new
        // state and a rule where seeing the attribute's name as a key in the YAML leads to entering that new
        // state. These rules are created here.

        for(let attribute of attributes_map.keys()){

            let new_state_name = `${attribute}-${current_state_name}`;
            let inner_map = new Map(Object.entries(attributes_map.get(attribute)));
            let content_type = inner_map.get("type");
            current_rules.push(
                {
                    token: "meta.tag",
                    regex: `(${attribute}(?=:))`,
                    next: new_state_name,
                    onMatch: function(val, state, stack) {
                        addToStatesMap(new_state_name, current_state_name, content_type, nested_level);
                        return this.token;
                    }
                }
            )
        }

        // Since the new state again needs its rules generated, a recursive call to mapToRules
        // for these new states is made.
        mapToRules(attributes_map, current_state_name, nested_level + 1)

        // since within objects only specific attributes are allowed, all values that do not match any of them, 
        // should be marked as invalid. That is what the invalidRule does.
        current_rules.push(invalidRule);
        return current_rules;
    }

    function rulesForYaml(current_rules, current_state_name){
        current_rules = current_rules.concat(yamlRules);
        return current_rules;
    }

    function rulesForJava(current_rules){
        current_rules = current_rules.concat(javaRules);
        return current_rules;
    }

    function rulesForEnum(current_rules, current_state_name, enum_values){
        current_rules.push(
            // the first and second rule are needed for the limited JSON object support, so enums are correctly
            // detected with braces or a comma after it too.
            // Instead of only matching the enum-value itself, the rule looks at the character afterwards too, so
            // it's not possible to write multiple of the possible enum-values at the same place in the YAML without
            // them being marked invalid.
            {
                token: ["variable.enum", "paren.rparen"],
                regex:  `(${enum_values.join("|")})(\})`
            }, {
                token: ["variable.enum", "text"],
                regex:  `(${enum_values.join("|")})(\,)`
            }, {
                token: "variable.enum",
                regex:  `((${enum_values.join("|")})(?=$))`
            }
        );
        // only possible enum-values should be allowed, so the invalidRule is added
        current_rules.push(invalidRule);
        return current_rules;
    }
    
    function mapToRulesTypeObject(inner_map, rules_for_current_key, current_state_name, nested_level){
        // If the current key being looked at stands for an object, this object's attributes are behind the key "object-attributes"
        let attributes_map = new Map(Object.entries(inner_map.get("object-attributes")));

        rules_for_current_key = rulesForObject(attributes_map, rules_for_current_key, current_state_name, nested_level);
        return rules_for_current_key;
    }

    // Generates the rules for values of the type Map.
    function mapToRulesTypeMap(inner_map, rules_for_current_key, current_state_name, nested_level){

        // Depending on whether Maps contain more InspectitConfig-specific objects
        // or not, e.g. Strings or int, as values, different rules need to be added.
        let map_content_type = inner_map.get("map-content-type");

        if((map_content_type === "object") || (map_content_type === "yaml")){

            // If the map contains InspectitConfig-specific objects or arbitrary YAML as values, a new state is needed that is entered
            // after seeing any key for the map.
            let sub_state_name = `single-${current_state_name}`

            // the rules for the new substate are created in mapToRulesMapSubkey
            mapToRulesMapSubkey(inner_map, sub_state_name, map_content_type, nested_level);

            // Any text is accepted as a key after which the new sub-state is entered for its values.
            rules_for_current_key.push(
                {
                    token: "variable",
                    regex:  /(.+?(?=:))/,
                    next: sub_state_name,
                    onMatch: function(val, state, stack) {
                        addToStatesMap(sub_state_name, current_state_name, map_content_type, nested_level);
                        return this.token;
                    }
                }
            );
            rules_for_current_key.push(
                {
                    token: "variable",
                    regex:  /(['][^']*?['](?=:))/,
                    next: sub_state_name,
                    onMatch: function(val, state, stack) {
                        addToStatesMap(sub_state_name, current_state_name, map_content_type, nested_level);
                        return this.token;
                    }
                }
            );

            // only keys with arbitrary names are allowed in this state, so the invalidRule is added
            rules_for_current_key.push(invalidRule);
            
        } else if(map_content_type === "text"){
            // if the map simply contains text content, no new sub-state is needed and instead, keys are simply
            // highlighted as variables again and the textRules are added to highlight any text in the values properly.
            rules_for_current_key.push(
                {
                    token: ["variable", "keyword"],
                    regex: /(['][^']*?[']\s*(?=:))(:)/
                },
                {
                    token: ["variable", "keyword"],
                    regex: /([^\s]+?\s*(?=:))(:)/
                }
            );
            rules_for_current_key = rules_for_current_key.concat(textRules);
        }

        return rules_for_current_key;
    }

    // Generates the rules for the contents of a Map if it contains InspectitConfig-specific objects.
    function mapToRulesMapSubkey(inner_map, sub_state_name, map_content_type, nested_level){

        // As before the list of rules for the state is created with the comment-rule already in it.
        let rules_for_sub_state = [commentRule, defaultRule, keywordRule, jsonStartRule]

        if(map_content_type === "object"){

            // If the map contains InspectitConfig-specific objects as values, these objects' attributes will be behind
            // the key "map-contents" in the inner_map.
            let contents_map = new Map(Object.entries(inner_map.get("map-contents")));

            rules_for_sub_state = rulesForObject(contents_map, rules_for_sub_state, sub_state_name, nested_level + 1)

        } else if (map_content_type === "yaml"){

            rules_for_sub_state = rulesForYaml(rules_for_sub_state, sub_state_name);

        }

        rules_for_sub_state = indentRules.concat(rules_for_sub_state);
        allRules.set(sub_state_name, rules_for_sub_state);
    }

    // Generates the rules for values of the type List.
    function mapToRulesTypeList(inner_map, rules_for_current_key, current_state_name, nested_level){

        // If the list contains InspectitConfig-specific objects as values, a new state is needed that is entered
        // after seeing the start of the list.
        let list_content_type = inner_map.get("list-content-type");
        if(list_content_type === "object"){

            let sub_state_name = `single-${current_state_name}`
            rules_for_current_key.push(
                {
                    token : "list.markup",
                    regex : /\s*[\-?](?:$|\s)/,
                    next: sub_state_name,
                    onMatch: function(val, state, stack) {
                        addToStatesMap(sub_state_name, current_state_name, list_content_type, nested_level);
                        return this.token;
                    }
                }
            );

            let rules_for_sub_state = []
            let contents_map = new Map(Object.entries(inner_map.get("list-contents")));

            rules_for_sub_state = rulesForObject(contents_map, rules_for_sub_state, sub_state_name, nested_level + 1);
            rules_for_sub_state = indentRules.concat(rules_for_sub_state);
            allRules.set(sub_state_name, rules_for_sub_state);

        } else if(inner_map.get("list-content-type") === "text"){

            // if the map simply contains text content, no new sub-state is needed and instead simply a rule to highlight
            // the list beginning correctly is needed and the textRules are added to highlight any text in the list
            rules_for_current_key.push(
                {
                    token: "list.markup",
                    regex: `- `,
                }
            );
            rules_for_current_key = rules_for_current_key.concat(textRules);

        }

        return rules_for_current_key;
    }

    mapToRules(rulesToGenerate, "", 1);

    // regexp must not have capturing parentheses. Use (?:) instead.
    // regexps are ordered -> the first match is used
    this.$rules = {
        "mlStringPre" : [
            {
                token : "indent",
                regex : /^ *$/
            }, {
                token : "indent",
                regex : /^ */,
                onMatch: function(val, state, stack) {
                    var curIndent = stack[1];
                    let last_state = stack[2];

                    if (curIndent >= val.length) {
                        let result = evaluateIndent(val, last_state);
                        this.next = result[0];
                        if(result[1] !== undefined){
                            return result[1];
                        }
                        stack.shift();
                        stack.shift();
                        stack.shift();
                    }
                    else {
                        stack[1] = val.length - 1;
                        this.next = stack[0] = "mlString";
                    }
                    return `${this.next}-indent.pre`;
                },
                next : "mlString"
            }, {
                defaultToken : "string"
            }
        ],
        "mlString" : [
            {
                token : "indent",
                regex : /^ *$/
            }, {
                token : "indent",
                regex : /^ */,
                onMatch: function(val, state, stack) {
                    var curIndent = stack[1];
                    let last_state = stack[2];

                    if (curIndent >= val.length) {
                        let result = evaluateIndent(val, last_state);
                        this.next = result[0];
                        if(result[1] !== undefined){
                            return result[1];
                        }
                        stack.splice(0);
                    }
                    else {
                        this.next = "mlString";
                    }
                    return `${this.next}-indent`;
                },
                next : "mlString"
            }, {
                token : "string",
                regex : '.+'
            }
        ],
        "indentationError" : [
            {
                token: "linebreak",
                regex: /$/,
                next: "pop"
            },
            {
                token: "invalid.illegal.indentationError",
                regex: /.*/,
            }
        ],
        "java-multi-line-pre": [
            {
                token : "indent",
                regex : /^ *$/
            }, {
                token : "indent",
                regex : /^ */,
                onMatch: function(val, state, stack) {
                    var curIndent = stack[1];
                    let last_state = stack[2];

                    if (curIndent >= val.length) {
                        let result = evaluateIndent(val, last_state);
                        this.next = result[0];
                        if(result[1] !== undefined){
                            return result[1];
                        }
                        stack.shift();
                        stack.shift();
                        stack.shift();
                    }
                    else {
                        stack[1] = val.length - 1;
                        this.next = stack[0] = "java-multiline-start";
                    }
                    return `${this.next}-indent.pre`;
                }
            }
        ],
        // this state is needed so comments in multiline Java-code are highlighted properly, because the comment state of
        // the actual JavaHighlightRules did not work correctly as is.
        "java-ml-comment-helper" : [
            {
                token : "java-ml-comment-helper-indent",
                regex : /^ *$/,
                next: "java-ml-comment-helper"
            }, {
                token : "indent",
                regex : /^ */,
                onMatch: function(val, state, stack) {
                    // push pushes back the values in stack by two indices:
                    // https://github.com/ajaxorg/ace/blob/v1.1.4/lib/ace/mode/text_highlight_rules.js#L112-L121
                    // so the curIndent and last_state values are now at 3 and 4 instead of 1 and 2
                    var curIndent = stack[3];
                    let last_state = stack[4];

                    if (curIndent >= val.length) {
                        let result = evaluateIndent(val, last_state);
                        this.next = result[0];
                        if(result[1] !== undefined){
                            return result[1];
                        }
                        stack.splice(0);
                    } else {
                        this.next = "java-ml-comment-helper";
                    }
                    return `${this.next}-indent.javaml`;
                }
            }, {
                token : "comment.helper", // closing comment
                regex : "\\*\\/",
                next : "pop"
            }, {
                defaultToken : "comment"
            }
        ]
    };
    this.$rules = Object.assign(this.$rules, Object.fromEntries(allRules));

    // For Java highlighting the JavaHighlightRules are embedded into the rules.

    // Once for multi-line Java:
    // Rules to deal with indentation like in multiline Strings are added to deal with indentation inside of the Java
    // multiline states, and a rule to enter the comment-helper state is added for multi-line comments.
    this.embedRules(JavaHighlightRules, "java-multiline-", [
        {
            token : "java-ml-indent",
            regex : /^ *$/,
            next: "java-multiline-start"
        }, {
            token : "indent",
            regex : /^ */,
            onMatch: function(val, state, stack) {
                var curIndent = stack[1];
                let last_state = stack[2];

                if (curIndent >= val.length) {
                    let result = evaluateIndent(val, last_state);
                    this.next = result[0];
                    if(result[1] !== undefined){
                        return result[1];
                    }
                    stack.splice(0);
                } else {
                    this.next = "java-multiline-start";
                }
                return `${this.next}-indent.javaml`;
            }
        }, {
            token : "comment.helper", // multi line comment
            regex : "\\/\\*",
            push : "java-ml-comment-helper"
        }
    ]);

    // Once for single-line Java.
    // A rule is added to exit from the Java state into the previous state at end-of-line.
    this.embedRules(JavaHighlightRules, "java-singleline-", [{
        token : "java",
        regex : /$/,
        next: "pop"
    }]);

    this.normalizeRules();

};

oop.inherits(InspectitOcelotHighlightRules, TextHighlightRules);

export default InspectitOcelotHighlightRules;