package caevo.parser;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.trees.LabeledScoredTreeNode;
import edu.stanford.nlp.trees.Tree;
import opennlp.tools.cmdline.parser.ParserTool;
import opennlp.tools.parser.Parse;
import opennlp.tools.parser.Parser;
import opennlp.tools.parser.ParserFactory;
import opennlp.tools.parser.ParserModel;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * This class implements the bridge between the OpenNLP parser product and that of the Stanford parser.
 *
 * @author ymishory
 */
public class OpenNlpParserAdapter extends ParserAdapter {

    private Parser parser = null;

    @Override
    public void init(String... args) throws IOException {
        InputStream modelInputStream = new FileInputStream(args[0]);
        ParserModel parserModel = new ParserModel(modelInputStream);
        parser = ParserFactory.create(parserModel);
    }

    @Override
    public Tree parseTree(List<HasWord> sentenceWords) {
        // no choice but to reconnect the tokens into a single string
        String sentence = getTextFromHasWordsList(sentenceWords);

        Parse result = ParserTool.parseLine(sentence, parser, 1)[0];

        Tree root = createRootNode();
        Queue<HasWord> leavesQ = new LinkedList<>(sentenceWords);
        Tree child = convertOpenNlpToStanfordTree(result.getChildren()[0], leavesQ);

        root.setChildren(new Tree[]{child});
        return root;
    }

    private Tree convertOpenNlpToStanfordTree(Parse root, Queue<HasWord> leavesQ) {
        // convert the root
        Tree rootNode = convertOpenNlpToStanfordNode(root, leavesQ);
        // prepare a child node for every child
        Tree[] children = new Tree[root.getChildCount()];
        for (int i = 0; i < root.getChildren().length; i++) {
            children[i] = convertOpenNlpToStanfordTree(root.getChildren()[i], leavesQ);
        }
        rootNode.setChildren(children);
        return rootNode;
    }

    private LabeledScoredTreeNode convertOpenNlpToStanfordNode(Parse p, Queue<HasWord> leavesQ) {
        CoreLabel newLabel = new CoreLabel();
        if (p.getChildCount() > 0) {
            // inner node
            newLabel.setTag(p.getType());
            newLabel.setBeginPosition(-1);
            newLabel.setEndPosition(-1);
            newLabel.setCategory(p.getType());
            newLabel.setValue(p.getType());
        } else {
            // leaf
            HasWord hasWord = leavesQ.remove();
            if (hasWord instanceof CoreLabel) {
                newLabel = (CoreLabel) hasWord;
            } else {
                newLabel.setValue(p.getCoveredText());
                newLabel.setWord(p.getCoveredText());
                newLabel.setOriginalText(p.getCoveredText());
                newLabel.setBeginPosition(p.getSpan().getStart());
                newLabel.setEndPosition(p.getSpan().getEnd());
                newLabel.setBefore(" ");
                newLabel.setAfter(" ");
            }
        }
        return new LabeledScoredTreeNode(newLabel);
    }

    private String getTextFromHasWordsList(List<HasWord> hasWordList) {
        StringBuilder sb = new StringBuilder();
        for (HasWord hasWord : hasWordList) {
            sb.append(hasWord.word()).append(" ");
        }
        return sb.toString().trim();
    }
}
