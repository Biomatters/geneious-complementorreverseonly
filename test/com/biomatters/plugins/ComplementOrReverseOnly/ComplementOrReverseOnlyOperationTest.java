package com.biomatters.plugins.ComplementOrReverseOnly;

import com.biomatters.geneious.publicapi.documents.AnnotatedPluginDocument;
import com.biomatters.geneious.publicapi.documents.DocumentUtilities;
import com.biomatters.geneious.publicapi.documents.sequence.NucleotideSequenceDocument;
import com.biomatters.geneious.publicapi.documents.sequence.SequenceAnnotation;
import com.biomatters.geneious.publicapi.documents.sequence.SequenceAnnotationInterval;
import com.biomatters.geneious.publicapi.implementations.sequence.DefaultNucleotideSequence;
import com.biomatters.geneious.publicapi.plugin.DocumentImportException;
import com.biomatters.geneious.publicapi.plugin.DocumentOperationException;
import com.biomatters.geneious.publicapi.plugin.Options;
import jebl.util.ProgressListener;
import junit.framework.TestCase;

import java.util.List;

/**
 * Copyright (C) 2013-2014, Biomatters Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
public class ComplementOrReverseOnlyOperationTest extends TestCase {
    public void testReverseOnly() throws DocumentImportException, DocumentOperationException {
        testIt(false);
    }

    public void testComplementOnly() throws DocumentImportException, DocumentOperationException {
        testIt(true);
    }

    public void testIt(boolean isComplementOnlyRatherThanReverseOnly) throws DocumentImportException, DocumentOperationException {
        DefaultNucleotideSequence source = new DefaultNucleotideSequence("seq", "GATTACA");
        SequenceAnnotation annotation = new SequenceAnnotation("a", "b", new SequenceAnnotationInterval(2, 3));
        source.addSequenceAnnotation(annotation);
        AnnotatedPluginDocument doc = DocumentUtilities.createAnnotatedPluginDocument(source);
        ComplementOrReverseOnlyOperation op = new ComplementOrReverseOnlyOperation("a","b",isComplementOnlyRatherThanReverseOnly);
        Options options = op.getOptions(doc);
        List<AnnotatedPluginDocument> results = op.performOperation(ProgressListener.EMPTY, options, doc);
        NucleotideSequenceDocument result = (NucleotideSequenceDocument) getSingleton(results).getDocument();
        if (isComplementOnlyRatherThanReverseOnly) {
            assertEquals("seq (complement without reverse)",result.getName());
            assertEquals("CTAATGT",result.getSequenceString());
            assertEquals(annotation,getSingleton(result.getSequenceAnnotations()));
        }
        else {
            assertEquals("seq (reverse without complement)",result.getName());
            assertEquals("ACATTAG",result.getSequenceString());
            List<SequenceAnnotation> sequenceAnnotations = result.getSequenceAnnotations();
            assertEquals(2,sequenceAnnotations.size());//includes an extraction annotation
            SequenceAnnotation resultAnnotation = sequenceAnnotations.get(0);
            if (!resultAnnotation.getType().equals("b"))
                resultAnnotation = sequenceAnnotations.get(1);
            assertEquals(new SequenceAnnotation("a","b",new SequenceAnnotationInterval(6,5)), resultAnnotation);
        }
    }

    public static <T> T getSingleton(List<T> list) {
        assertEquals(1, list.size());
        return list.get(0);
    }
}
