package caevo.parser;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.trees.Tree;
import opennlp.tools.parser.Parser;
import opennlp.tools.parser.ParserFactory;
import opennlp.tools.parser.ParserModel;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * todo
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
    public Tree parseTree(List<HasWord> sentence) {
        // todo
        return null;
    }
}
