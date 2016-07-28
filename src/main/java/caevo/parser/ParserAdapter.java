package caevo.parser;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.trees.Tree;

import java.util.List;

/**
 * This abstract class provides an API for CAEVO to access a linguistic parser, without having to know the parser
 * implementation, thus allowing the parser to be a pluggable component of the system, configurable without need for
 * code changes.
 *
 * @author ymishory
 */
public abstract class ParserAdapter {

    public abstract void init(String... args) throws Exception;

    public ParserAdapter() {
    }

    public abstract Tree parseTree(List<HasWord> sentence);

}
