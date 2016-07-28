package caevo.parser;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.trees.Tree;

import java.util.List;

/**
 * This class implements the adapter for the Stanford parser.
 *
 * @author ymishory
 */
public class StanfordParserAdapter extends ParserAdapter {

    private LexicalizedParser parser = null;

    public StanfordParserAdapter() {
    }

    @Override
    public Tree parseTree(List<HasWord> sentence) {
        return parser.parseTree(sentence);
    }

    public void init(String... args) {
        this.parser = LexicalizedParser.loadModel(args[0]);
    }
}
