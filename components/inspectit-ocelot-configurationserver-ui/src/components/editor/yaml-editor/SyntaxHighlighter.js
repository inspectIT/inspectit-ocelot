/*Any mentions of levelNames[(val.length/2)] would need to change depending on spaces for tab indent in the editor*/

define(function(require, exports, module) {
    "use strict";

    var oop = require("../lib/oop");
    var TextHighlightRules = require("./text_highlight_rules").TextHighlightRules;


    var YamlHighlightRules = function() {

        const baseOldRules = [
            {
                token : "comment",
                regex : "#.*$"
            }, {
                token : "list.markup",
                regex : /^(?:-{3}|\.{3})\s*(?=#|$)/
            },  {
                token : "list.markup",
                regex : /^\s*[\-?](?:$|\s)/
            }, {
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
            }, {
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
            }];

        const keywordRule = [
            {
                token: "keyword",
                regex: /:\s*/
            }];

        const invalidRule = [
            {
                token: "invalid.illegal",
                regex: /.*/,
                onMatch: function(val, state, stack) {
                    return `invalid.illegal.${state}`;
                }
            }
        ]

        const allBaseRules = baseOldRules.concat(keywordRule).concat(invalidRule);

        const exitEntryRules =
            {
                "map-content-type": "object",
                "map-contents": {
                    "action": {
                        "type": "text"
                    },
                    "only-if-true": {
                        "type": "text"
                    },
                    "only-if-null": {
                        "type": "text"
                    },
                    "only-if-not-null": {
                        "type": "text"
                    },
                    "only-if-false": {
                        "type": "text"
                    },
                    "data-input": {
                        "map-content-type": "text"
                    },
                    "constant-input": {
                        "map-content-type": "yaml"
                    },
                    "order": {
                        "object-attributes": {
                            "reads-before-written": {
                                "type": "text"
                            },
                            "reads": {
                                "type": "text"
                            },
                            "writes": {
                                "type": "text"
                            }
                        }
                    }
                }
            };

        const rulesToGenerate = new Map(Object.entries({
            "start": {
                "object-attributes": {
                    "inspectit": {
                        "object-attributes": {
                            "instrumentation": {
                                "object-attributes": {
                                    "actions": {
                                        "map-content-type": "object",
                                        "map-contents": {
                                            "imports": {
                                                "list-content-type": "text"
                                            },
                                            "input": {
                                                "map-content-type": "text"
                                            },
                                            "is-void": {
                                                "type": "text"
                                            },
                                            "value-body": {
                                                "type": "java"
                                            },
                                            "value": {
                                                "type": "java"
                                            }
                                        }
                                    },
                                    "scopes": {
                                        "map-content-type": "object",
                                        "map-contents": {
                                            "superclass": {
                                                "object-attributes": {
                                                    "name": {
                                                        "type": "text"
                                                    }
                                                }
                                            },
                                            "interfaces": {
                                                "list-content-type": "object",
                                                "list-contents": {
                                                    "name": {
                                                        "type": "text"
                                                    },
                                                    "arguments": {
                                                        "type": "text"
                                                    }
                                                }
                                            },
                                            "methods": {
                                                "list-content-type": "object",
                                                "list-contents": {
                                                    "name": {
                                                        "type": "text"
                                                    },
                                                    "arguments": {
                                                        "type": "text"
                                                    }
                                                }
                                            },
                                            "advanced": {
                                                "object-attributes": {
                                                    "instrument-only-inherited-methods": {
                                                        "type": "text"
                                                    },
                                                    "disable-safety-mechanisms": {
                                                        "type": "text"
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    "rules": {
                                        "map-content-type": "object",
                                        "map-contents": {
                                            "include": {
                                                "type": "text"
                                            },
                                            "scopes": {
                                                "type": "text"
                                            },
                                            "metrics": {
                                                "map-content-type": "object",
                                                "map-contents": {
                                                    "value": {
                                                        "type": "text"
                                                    },
                                                    "metric": {
                                                        "type": "text"
                                                    },
                                                    "data-tags": {
                                                        "map-content-type": "text"
                                                    },
                                                    "constant-tags": {
                                                        "map-content-type": "text"
                                                    }
                                                }
                                            },
                                            "tracing": {
                                                "object-attributes": {
                                                    "start-span-conditions": {
                                                        "object-attributes": {
                                                            "only-if-true": {
                                                                "type": "text"
                                                            },
                                                            "only-if-null": {
                                                                "type": "text"
                                                            },
                                                            "only-if-not-null": {
                                                                "type": "text"
                                                            },
                                                            "only-if-false": {
                                                                "type": "text"
                                                            }
                                                        }
                                                    },
                                                    "attributes": {
                                                        "map-content-type": "text"
                                                    },
                                                    "start-span": {
                                                        "type": "text"
                                                    },
                                                    "kind": {
                                                        "type": "text"
                                                    },
                                                    "auto-tracing": {
                                                        "type": "text"
                                                    },
                                                    "store-span": {
                                                        "type": "text"
                                                    },
                                                    "error-status": {
                                                        "type": "text"
                                                    },
                                                    "end-span": {
                                                        "type": "text"
                                                    }
                                                }
                                            },
                                            "entry": exitEntryRules,
                                            "exit": exitEntryRules,
                                            "pre-exit": exitEntryRules,
                                            "post-exit": exitEntryRules,
                                            "pre-entry": exitEntryRules,
                                            "post-entry": exitEntryRules,
                                            "advanced": {
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }));

        function getIndentRulesArbitraryYaml(current_level_name){
            return [
                {
                    token : "indent",
                    regex : /^ *$/,
                    onMatch: function(val, state, stack) {
                        return `${current_level_name}-indent`;
                    }
                }, {
                    token : "indent",
                    regex : /^ */,
                    onMatch: function(val, state, stack) {
                        let expected_indent = levels.get(current_level_name).get("indent");

                        if (val.length % 2 !== 0){
                            this.next = current_level_name;
                            return `invalid.illegal.${state}`;
                        } else if(val.length < expected_indent){
                            let levels_up = (expected_indent - val.length) / 2;
                            let parent_levels = levels.get(current_level_name).get("parents");
                            let new_level = parent_levels[parent_levels.length - levels_up];
                            this.next = new_level;
                            return `${this.next}-indent`;
                        } else {
                            this.next = current_level_name;
                            return `${current_level_name}-indent`;
                        }
                    }
                }];
        }

        function evaluateIndent(val, level_name){

            let expected_indent = levels.get(level_name).get("indent");
            if ((val.length % 2 !== 0) || (val.length > expected_indent)){
                return [level_name, `invalid.illegal.indentationError`];
            } else if(val.length === expected_indent){
                return [level_name, undefined];
            } else {
                let parent_levels = levels.get(level_name).get("parents");
                let levels_up = (expected_indent - val.length) / 2;
                let new_level = parent_levels[parent_levels.length - levels_up];
                return [new_level, undefined];
            }

        }

        function getIndentRules(current_level_name){
            return [
                {
                    token : "indent",
                    regex : /^ *$/,
                    onMatch: function(val, state, stack) {
                        return `${current_level_name}-indent`;
                    }
                }, {
                    token : "indent",
                    regex : /^ */,
                    onMatch: function(val, state, stack) {
                        let result = evaluateIndent(val, current_level_name);
                        if(result[1] !== undefined){
                            this.next = "indentationError";
                            stack.unshift(this.next, state);
                            return result[1];
                        } else {
                            this.next = result[0];
                            return `${this.next}-indent`;
                        }
                    }
                }];
        }

        function getStringMultiline(current_level_name){
            return {
                token : "string.mlStart", // multi line string start
                regex : /[|>][-+\d]*(?:$|\s+(?:$|#))/,
                onMatch: function(val, state, stack, line) {
                    line = line.replace(/ #.*/, "");
                    var indent = /^ *((:\s*)?-(\s*[^|>])?)?/.exec(line)[0]
                        .replace(/\S\s*$/, "").length;
                    var indentationIndicator = parseInt(/\d+[\s+-]*$/.exec(line));

                    if (indentationIndicator) {
                        indent += indentationIndicator - 1;
                        this.next = "mlString";
                    } else {
                        this.next = "mlStringPre";
                    }
                    if (!stack.length) {
                        stack.push(this.next);
                        stack.push(indent);
                        stack.push(current_level_name);
                    } else {
                        stack[0] = this.next;
                        stack[1] = indent;
                        stack[2] = current_level_name;
                    }
                    return this.token;
                },
                next : "mlString"
            }
        }


        let allRules = new Map();

        const levels = new Map();
        const start_entry = new Map();
        start_entry.set("indent", 0);
        start_entry.set("parents", []);
        levels.set("start", start_entry);

        function addToLevelsMap(level_name, parent_name, counter){
            let parent_parents = levels.get(parent_name).get("parents");
            let all_parents = parent_parents.concat([parent_name]);
            let level_entry = new Map();
            level_entry.set("indent", counter * 2);
            level_entry.set("parents", all_parents);
            levels.set(level_name, level_entry);
        }

        function mapToRules(map, parent_key, parent_name, counter){

            for(let key of map.keys()){

                let level_content_is_text = false;

                let current_level_name;
                if(key==="start"){
                    current_level_name = key;
                } else {
                    current_level_name = `${parent_key}-${key}`;
                }

                let rules_for_current_key = []
                let inner_map = new Map(Object.entries(map.get(key)));
                if(inner_map.has("object-attributes")){

                    let attributes_map = new Map(Object.entries(inner_map.get("object-attributes")));
                    for(let attribute of attributes_map.keys()){

                        let level_name = `${key}-${attribute}`;
                        rules_for_current_key.push(
                            {
                                token: "meta.tag",
                                regex: `(${attribute}(?=:))`,
                                next: level_name,
                                onMatch: function(val, state, stack) {
                                    addToLevelsMap(level_name, current_level_name, counter);
                                    return this.token;
                                }
                            }
                        )
                    }
                    mapToRules(attributes_map, key, current_level_name, counter + 1)

                } else if (inner_map.has("map-content-type")){

                    let map_content_type = inner_map.get("map-content-type");
                    if((map_content_type === "object") || (map_content_type === "yaml")){

                        let sub_key = `single-${key}`
                        rules_for_current_key.push(
                            {
                                token: "variable",
                                regex:  /(['][^']*?['](?=:))/,
                                next: sub_key,
                                onMatch: function(val, state, stack) {
                                    addToLevelsMap(sub_key, current_level_name, counter);
                                    return this.token;
                                }
                            }
                        );
                        rules_for_current_key.push(
                            {
                                token: "variable",
                                regex:  /(.+?(?=:))/,
                                next: sub_key,
                                onMatch: function(val, state, stack) {
                                    addToLevelsMap(sub_key, current_level_name, counter);
                                    return this.token;
                                }
                            }
                        );

                        let rules_for_sub_key = []

                        if(map_content_type === "object"){
                            let contents_map = new Map(Object.entries(inner_map.get("map-contents")));
                            for(let content of contents_map.keys()){

                                let level_name = `${sub_key}-${content}`;
                                rules_for_sub_key.push(
                                    {
                                        token: "meta.tag",
                                        regex: `(${content}(?=:))`,
                                        next: level_name,
                                        onMatch: function(val, state, stack) {
                                            addToLevelsMap(level_name, sub_key, counter + 1);
                                            return this.token;
                                        }
                                    }
                                );
                            }
                            rules_for_sub_key = getIndentRules(sub_key).concat(rules_for_sub_key);
                            mapToRules(contents_map, sub_key, sub_key, counter + 2);
                            allRules.set(sub_key, rules_for_sub_key.concat(allBaseRules));
                        } else {
                            rules_for_sub_key = getIndentRulesArbitraryYaml(sub_key).concat(rules_for_sub_key);
                            rules_for_sub_key.push(getStringMultiline(sub_key));

                            /*needed because the meta.tag rule was removed from the standard yaml
                            highlight rules to mark wrong tags, but there are no wrong tags possible here.*/
                            rules_for_sub_key.push(
                                {
                                    token: ["meta.tag", "keyword"],
                                    regex: /(\w+?)(\s*\:(?:\s+|$))/
                                },

                                /*needed because standard list rule expects the list to be matched with the
                                beginning of the line and the whitespaces between, but those are already handled
                                and tokenized as indents, so we need a list rule that works without the beginning
                                of the line.*/
                                {
                                    token : "list.markup",
                                    regex : /\s*[\-?](?:$|\s)/
                                }
                            );
                            rules_for_sub_key = rules_for_sub_key.concat(baseOldRules.concat(keywordRule));
                            allRules.set(sub_key, rules_for_sub_key);
                        }


                    } else if(map_content_type === "text"){
                        rules_for_current_key.push(
                            {
                                token: ["variable", "keyword"],
                                regex: /(['](?:(?:[^']))*?['](?=:))(:)/
                            },
                            {
                                token: ["variable", "keyword"],
                                regex: /(.+?(?=:))/
                            }
                        );

                        level_content_is_text = true;
                    }

                } else if (inner_map.has("list-content-type")){

                    if(inner_map.get("list-content-type") === "object"){

                        let sub_key = `single-${key}`
                        rules_for_current_key.push(
                            {
                                token : "list.markup",
                                regex : /\s*[\-?](?:$|\s)/,
                                next: sub_key,
                                onMatch: function(val, state, stack) {
                                    addToLevelsMap(sub_key, current_level_name, counter);
                                    return this.token;
                                }
                            }
                        );
                        let rules_for_sub_key = []

                        let contents_map = new Map(Object.entries(inner_map.get("list-contents")));
                        for(let content of contents_map.keys()){

                            let level_name = `${sub_key}-${content}`;
                            rules_for_sub_key.push(
                                {
                                    token: "meta.tag",
                                    regex: `(${content}(?=:))`,
                                    next: level_name,
                                    onMatch: function(val, state, stack) {
                                        addToLevelsMap(level_name, sub_key, counter + 1);
                                        return this.token;
                                    }
                                }
                            );
                        }
                        rules_for_sub_key = getIndentRules(sub_key).concat(rules_for_sub_key);
                        mapToRules(contents_map, sub_key, sub_key, counter + 2);
                        allRules.set(sub_key, rules_for_sub_key.concat(allBaseRules));

                    } else if(inner_map.get("list-content-type") === "text"){

                        rules_for_current_key.push(
                            {
                                token: "list.markup",
                                regex: `- `,
                            }
                        );
                        level_content_is_text = true;

                    }

                } else {
                    let type = inner_map.get("type");
                    if(type==="java"){
                        rules_for_current_key.push(
                            {
                                token: "keyword.java-sl-start",
                                regex: /([:](?=(?:[^\||'|"|"""])*$))/,
                                push: "java-singleline-start"
                            }, {
                                token: "text.java-sl-start",
                                regex: /('|"|""")/,
                                push: "java-singleline-start"
                            }, {
                                token : "text.java-multi-line-start", // multi line string start
                                regex : /[|>][-+\d]*(?:$|\s+(?:$|#))/,
                                onMatch: function(val, state, stack, line) {
                                    line = line.replace(/ #.*/, "");
                                    var indent = /^ *((:\s*)?-(\s*[^|>])?)?/.exec(line)[0]
                                        .replace(/\S\s*$/, "").length;
                                    var indentationIndicator = parseInt(/\d+[\s+-]*$/.exec(line));

                                    if (indentationIndicator) {
                                        indent += indentationIndicator - 1;
                                        this.next = "java-multi-line-start";
                                    } else {
                                        this.next = "java-multi-line-pre";
                                    }
                                    if (!stack.length) {
                                        stack.push(this.next);
                                        stack.push(indent);
                                        stack.push(current_level_name);
                                    } else {
                                        stack[0] = this.next;
                                        stack[1] = indent;
                                        stack[2] = current_level_name;
                                    }
                                    return this.token;
                                }
                            }

                        )
                    } else {
                        level_content_is_text = true;
                    }
                }

                if(key !== "start"){
                    rules_for_current_key = getIndentRules(current_level_name).concat(rules_for_current_key);
                }

                rules_for_current_key.push(getStringMultiline(current_level_name));

                if(level_content_is_text){
                    rules_for_current_key = rules_for_current_key.concat(baseOldRules.concat(keywordRule));
                    allRules.set(current_level_name, rules_for_current_key);
                } else {
                    allRules.set(current_level_name, rules_for_current_key.concat(allBaseRules))
                }
            }
        }

        mapToRules(rulesToGenerate, "", "", 1);

        var JavaHighlightRules = require("ace/mode/java_highlight_rules").JavaHighlightRules;


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
                        let last_level = stack[2];

                        if (curIndent >= val.length) {
                            let result = evaluateIndent(val, last_level);
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
                        let last_level = stack[2];

                        if (curIndent >= val.length) {
                            let result = evaluateIndent(val, last_level);
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
                        let last_level = stack[2];

                        if (curIndent >= val.length) {
                            let result = evaluateIndent(val, last_level);
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
            "java-ml-comment-helper" : [
                {
                    token : "java-ml-comment-helper-indent",
                    regex : /^ *$/,
                    next: "java-ml-comment-helper"
                }, {
                    token : "indent",
                    regex : /^ */,
                    onMatch: function(val, state, stack) {
                        //push pushes back the values in stack by two indices:
                        //https://github.com/ajaxorg/ace/blob/v1.1.4/lib/ace/mode/text_highlight_rules.js#L112-L121
                        var curIndent = stack[3];
                        let last_level = stack[4];

                        if (curIndent >= val.length) {
                            let result = evaluateIndent(val, last_level);
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
                    let last_level = stack[2];

                    if (curIndent >= val.length) {
                        let result = evaluateIndent(val, last_level);
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

        this.embedRules(JavaHighlightRules, "java-singleline-", [{
            token : "java",
            regex : /$/,
            next: "pop"
        }]);

        this.normalizeRules();

    };

    oop.inherits(YamlHighlightRules, TextHighlightRules);

    exports.YamlHighlightRules = YamlHighlightRules;
});