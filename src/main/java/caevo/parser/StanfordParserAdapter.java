package caevo.parser;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.trees.Tree;

import java.util.List;

/**
 * This class implements the (trivial) adapter for the Stanford parser.
 *
 * @author ymishory
 */
public class StanfordParserAdapter extends ParserAdapter {

    private static final String DEFAULT_GRAMMAR_FILE = "edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz";
    private LexicalizedParser parser = null;

    public StanfordParserAdapter() {
    }

    @Override
    public Tree parseTree(List<HasWord> sentence) {
        return parser.parseTree(sentence);
    }

    public void init(String... args) {
        if (args.length > 0 && args[0] != null)
            this.parser = LexicalizedParser.loadModel(args[0]);
        else
            this.parser = LexicalizedParser.loadModel(DEFAULT_GRAMMAR_FILE);
    }
}
