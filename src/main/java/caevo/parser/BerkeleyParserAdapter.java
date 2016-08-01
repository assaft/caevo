package caevo.parser;

import edu.berkeley.nlp.PCFGLA.CoarseToFineMaxRuleParser;
import edu.berkeley.nlp.PCFGLA.Grammar;
import edu.berkeley.nlp.PCFGLA.Lexicon;
import edu.berkeley.nlp.PCFGLA.ParserData;
import edu.berkeley.nlp.util.Numberer;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.trees.LabeledScoredTreeNode;
import edu.stanford.nlp.trees.Tree;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * todo
 */
public class BerkeleyParserAdapter extends ParserAdapter {

    private CoarseToFineMaxRuleParser parser;
    private static final String DEFAULT_GRAMMAR_FILE = "lib/eng_sm6.gr";

    @Override
    public void init(String... args) {
        ParserData parserData;
        if (args.length > 0 && args[0] != null) {
            parserData = ParserData.Load(args[0]);
        } else {
            parserData = ParserData.Load(DEFAULT_GRAMMAR_FILE);
        }
        Grammar grammar = parserData.getGrammar();
        Lexicon lexicon = parserData.getLexicon();
        Numberer.setNumberers(parserData.getNumbs());

        parser = new CoarseToFineMaxRuleParser(grammar, lexicon, 1.0, -1, false, false, false, true, false, false, true);
    }

    @Override
    public Tree parseTree(List<HasWord> hasWordList) {
        List<String> sentence = getStringListFromHasWordList(hasWordList);
        // parse!
        edu.berkeley.nlp.syntax.Tree<String> parsedTree =
                parser.getBestConstrainedParse(sentence, null, false);

        Tree root = createRootNode();
        Queue<HasWord> leavesQ = new LinkedList<>(hasWordList);
        Tree child = convertBerkeleyToStanfordTree(parsedTree.getChild(0), leavesQ);

        root.setChildren(new Tree[]{child});
        return root;
    }

    private Tree convertBerkeleyToStanfordTree(edu.berkeley.nlp.syntax.Tree<String> root, Queue<HasWord> leavesQ) {
        Tree rootNode = covertBerkeleyNodeToStanfordNode(root, leavesQ);
        Tree[] children = new Tree[root.getChildren().size()];
        for (int i = 0; i < root.getChildren().size(); i++) {
            children[i] = convertBerkeleyToStanfordTree(root.getChild(i), leavesQ);
        }
        rootNode.setChildren(children);
        return rootNode;
    }

    private Tree covertBerkeleyNodeToStanfordNode(edu.berkeley.nlp.syntax.Tree<String> bNode, Queue<HasWord> leavesQ) {
        CoreLabel newLabel = new CoreLabel();
        if (bNode.isLeaf()) {
            HasWord hasWord = leavesQ.remove();
            if (hasWord instanceof CoreLabel) {
                newLabel = (CoreLabel) hasWord;
            } else {
                newLabel.setValue(bNode.getLabel());
                newLabel.setWord(bNode.getLabel());
                newLabel.setOriginalText(bNode.getLabel());
                newLabel.setBefore(" ");
                newLabel.setAfter(" ");
            }
        } else {
            // inner node
            newLabel.setTag(bNode.getLabel());
            newLabel.setBeginPosition(-1);
            newLabel.setEndPosition(-1);
            newLabel.setCategory(bNode.getLabel());
            newLabel.setValue(bNode.getLabel());
        }

        return new LabeledScoredTreeNode(newLabel);
    }

    private static List<String> getStringListFromHasWordList(List<HasWord> hasWordList) {
        List<String> result = new ArrayList<>(hasWordList.size());
        for (HasWord hasWord : hasWordList) {
            result.add(hasWord.word());
        }
        return result;
    }
}
