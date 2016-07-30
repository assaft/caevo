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

    /**
     * Initialization method for the adapter and any field it might need to initialize. Override this method with
     * specific initialization code.
     *
     * @param args String arguments needed for initialization. Contents of the arguments is implementation-specific.
     * @throws Exception In case initialization fails for some reason.
     */
    public void init(String... args) throws Exception {
    }

    /**
     * Zero-argument constructor, for instantiation through reflection. Use this constructor in conjunction with the
     * {@link #init(String...) init} method.
     */
    public ParserAdapter() {
    }

    /**
     * This method receives input in the Stanford-specific form of a list of {@link HasWord}, parses the tokenized
     * sentence, and returns output in the Stanford-specific form of a {@link Tree}.
     * @param sentence A tokenized sentence, in the form of a list of {@link HasWord} objects.
     * @return A parse tree in the form of a {@link Tree}
     */
    public abstract Tree parseTree(List<HasWord> sentence);

}
