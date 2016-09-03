package caevo.sieves;

import caevo.*;
import caevo.tlink.EventTimeLink;
import caevo.tlink.TLink;

import java.util.LinkedList;
import java.util.List;

/**
 * This sieve tries to link the DCT to yet-unlinked events, according to their tense (assuming DCT is the present).
 */
public class CreationTimeLinksSieve implements Sieve {
    @Override
    public List<TLink> annotate(SieveDocument doc, List<TLink> currentTLinks) {
        List<TLink> proposed = new LinkedList<TLink>();
        if (doc.getDocstamp() == null || doc.getDocstamp().isEmpty()) return proposed;
        // currently supporting a single DCT; fixme
        Timex dct = doc.getDocstamp().get(0);
        for (SieveSentence sentence : doc.getSentences()) {
            //events
            for (TextEvent event : sentence.events()) {
                if (!linkExists(dct, event.getEiid(), currentTLinks)) {
                    TextEvent.Tense tense = event.getTense();
                    if (tense != null) {
                        switch (tense) {
                            case PAST:
                            case PASTPART:
                                proposed.add(new EventTimeLink(event.getEiid(), dct.getTid(), TLink.Type.BEFORE));
                                break;
                            case PRESENT:
                            case PRESPART:
                                proposed.add(new EventTimeLink(event.getEiid(), dct.getTid(), TLink.Type.IS_INCLUDED));
                                break;
                            case FUTURE:
                                proposed.add(new EventTimeLink(event.getEiid(), dct.getTid(), TLink.Type.AFTER));
                                break;
                        }
                    }
                }
            }
            // timexes are covered by the TimeTimeSieve
        }
        return proposed;
    }

    private boolean linkExists(Timex dct, String id, List<TLink> currentTLinks) {
        for (TLink link : currentTLinks) {
            if (link.getId1().equals(id) && link.getId2().equals(dct.getTid()) ||
                    link.getId1().equals(dct.getTid()) && link.getId2().equals(id))
                return true;
        }
        return false;
    }

    @Override
    public void train(SieveDocuments infoDocs) {
        return; // no training, only rules.
    }
}
