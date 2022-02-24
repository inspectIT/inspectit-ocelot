import ace from 'ace-builds/src-noconflict/ace';
import InspectitOcelotHighlightRules from './InspectitOcelotHighlightRules';

var oop = ace.require('ace/lib/oop');
// defines the parent mode
var TextMode = ace.require('ace/mode/text').Mode;
//var Tokenizer = ace.require('ace/tokenizer').Tokenizer;
var MatchingBraceOutdent = ace.require('ace/mode/matching_brace_outdent').MatchingBraceOutdent;
//var WorkerClient = ace.require('ace/worker/worker_client').WorkerClient;

// defines the language specific highlighters and folding rules
var FoldMode = ace.require('ace/mode/folding/coffee').FoldMode;

const InspectitOcelotMode = function () {
  this.HighlightRules = InspectitOcelotHighlightRules;
  this.$outdent = new MatchingBraceOutdent();
  this.foldingRules = new FoldMode();

  this.lineCommentStart = ['#'];

  this.getNextLineIndent = function (state, line, tab) {
    var indent = this.$getIndent(line);

    if (state === 'start') {
      var match = line.match(/^.*[{([]\s*$/);
      if (match) {
        indent += tab;
      }
    }

    return indent;
  };

  this.checkOutdent = function (state, line, input) {
    return this.$outdent.checkOutdent(line, input);
  };

  this.autoOutdent = function (state, doc, row) {
    this.$outdent.autoOutdent(doc, row);
  };
};
oop.inherits(InspectitOcelotMode, TextMode);

export default InspectitOcelotMode;
